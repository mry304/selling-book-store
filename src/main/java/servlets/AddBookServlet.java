package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.constant.db.BooksDBConstants;
import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB
    maxFileSize = 5 * 1024 * 1024,        // 5 MB
    maxRequestSize = 6 * 1024 * 1024      // 6 MB
)
public class AddBookServlet extends HttpServlet {
    BookService bookService = new BookServiceImpl();

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        res.setContentType("text/html; charset=UTF-8");
        PrintWriter pw = res.getWriter();

        if (!StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập quản trị để tiếp tục!</td></tr></table>");
            return;
        }

        RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
        rd.include(req, res);
        StoreUtil.setActiveTab(pw, "addbook");
        pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:28px;\">");
        pw.println("  <div class=\"bookshelf-page-container\" style=\"max-width:700px; margin: 0 auto;\">");
        pw.println("    <div class=\"bookshelf-page-card\" style=\"box-shadow: var(--shadow-canvas);\">");

        // Detect import request
        String action = req.getParameter("action");
        Part filePart = null;
        try {
            filePart = req.getPart("importFile");
        } catch (Exception ignored) {
            // Not a multipart request
        }

        if ("import".equalsIgnoreCase(action) || (filePart != null && filePart.getSize() > 0)) {
            handleBulkImport(pw, filePart);
            pw.println("    </div></div></main>");
            return;
        }

        // Single book flow
        String bName = req.getParameter(BooksDBConstants.COLUMN_NAME);
        if (bName == null || bName.trim().isEmpty()) {
            showAddBookForm(pw);
            pw.println("    </div></div></main>");
            return;
        }

        try {
            String uniqueID = UUID.randomUUID().toString();
            String bCode = uniqueID;
            String bAuthor = req.getParameter(BooksDBConstants.COLUMN_AUTHOR);
            String priceParam = req.getParameter(BooksDBConstants.COLUMN_PRICE);
            String qtyParam = req.getParameter(BooksDBConstants.COLUMN_QUANTITY);

            double bPrice = Double.parseDouble(priceParam);
            int bQty = Integer.parseInt(qtyParam);

            if (bPrice <= 0 || bQty < 0) {
                pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Đơn giá và số lượng phải lớn hơn 0!</div>");
                showAddBookForm(pw);
                pw.println("    </div></div></main>");
                return;
            }

            Book book = new Book(bCode, bName.trim(), bAuthor != null ? bAuthor.trim() : "", bPrice, bQty);
            String message = bookService.addBook(book);
            if ("SUCCESS".equalsIgnoreCase(message)) {
                pw.println("<div class='alert alert-success text-center' style='border-radius:12px; margin-bottom:24px;'>Đã thêm sách \"" + escapeHtml(bName) + "\" vào kho thành công!</div>");
                pw.println("<div class='text-center'><a href='storebooks' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary);'>&larr; Xem Kho Sách</a> <a href='addbook' class='nav-pill-btn' style='margin-left:12px;'>+ Thêm Cuốn Sách Khác</a></div>");
            } else {
                pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Thêm sách thất bại. Vui lòng kiểm tra lại các trường thông tin!</div>");
                showAddBookForm(pw);
            }
        } catch (NumberFormatException nfe) {
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Đơn giá hoặc số lượng không hợp lệ. Vui lòng nhập số chính xác!</div>");
            showAddBookForm(pw);
        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Đã xảy ra lỗi khi thêm sách. Vui lòng thử lại!</div>");
            showAddBookForm(pw);
        }
        pw.println("    </div></div></main>");
    }

    private void handleBulkImport(PrintWriter pw, Part filePart) {
        if (filePart == null || filePart.getSize() == 0) {
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Vui lòng chọn một file văn bản (.txt hoặc .csv) hợp lệ để tải lên!</div>");
            showAddBookForm(pw);
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        int lineNumber = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = filePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip blank lines or comment lines starting with #
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String delimiter = line.contains("|") ? "\\|" : ",";
                String[] parts = line.split(delimiter);

                String bName = "";
                String bAuthor = "";
                String priceStr = "";
                String qtyStr = "";

                if (delimiter.equals("\\|")) {
                    if (parts.length < 4) {
                        failureCount++;
                        errors.add("Dòng " + lineNumber + ": Không đủ 4 trường dữ liệu (Cần: Tên | Tác giả | Đơn giá | Số lượng). Nội dung: " + escapeHtml(line));
                        continue;
                    }
                    bName = parts[0].trim();
                    bAuthor = parts[1].trim();
                    priceStr = parts[2].trim();
                    qtyStr = parts[3].trim();
                } else {
                    if (parts.length == 4) {
                        bName = parts[0].trim();
                        bAuthor = parts[1].trim();
                        priceStr = parts[2].trim();
                        qtyStr = parts[3].trim();
                    } else if (parts.length > 4) {
                        // Tên sách có chứa dấu phẩy
                        qtyStr = parts[parts.length - 1].trim();
                        priceStr = parts[parts.length - 2].trim();
                        bAuthor = parts[parts.length - 3].trim();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i <= parts.length - 4; i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(parts[i].trim());
                        }
                        bName = sb.toString().trim();
                    } else {
                        failureCount++;
                        errors.add("Dòng " + lineNumber + ": Không đủ 4 trường dữ liệu (Cần: Tên, Tác giả, Đơn giá, Số lượng). Nội dung: " + escapeHtml(line));
                        continue;
                    }
                }

                if (bName.isEmpty() || bAuthor.isEmpty()) {
                    failureCount++;
                    errors.add("Dòng " + lineNumber + ": Tên sách hoặc tác giả không được để trống.");
                    continue;
                }

                double bPrice;
                int bQty;
                try {
                    bPrice = Double.parseDouble(priceStr);
                    bQty = Integer.parseInt(qtyStr);
                    if (bPrice <= 0 || bQty <= 0) {
                        failureCount++;
                        errors.add("Dòng " + lineNumber + " (\"" + escapeHtml(bName) + "\"): Đơn giá và số lượng phải lớn hơn 0.");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    failureCount++;
                    errors.add("Dòng " + lineNumber + " (\"" + escapeHtml(bName) + "\"): Đơn giá ('" + escapeHtml(priceStr) + "') hoặc số lượng ('" + escapeHtml(qtyStr) + "') không đúng định dạng số.");
                    continue;
                }

                try {
                    String bCode = UUID.randomUUID().toString();
                    Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
                    String resultMsg = bookService.addBook(book);
                    if ("SUCCESS".equalsIgnoreCase(resultMsg)) {
                        successCount++;
                    } else {
                        failureCount++;
                        errors.add("Dòng " + lineNumber + " (\"" + escapeHtml(bName) + "\"): Thêm vào cơ sở dữ liệu thất bại.");
                    }
                } catch (Exception e) {
                    failureCount++;
                    errors.add("Dòng " + lineNumber + " (\"" + escapeHtml(bName) + "\"): Lỗi hệ thống khi lưu - " + escapeHtml(e.getMessage()));
                }
            }

            // Render result report
            pw.println("<header class=\"bookshelf-page-header\" style=\"text-align:center;\">");
            pw.println("  <h1>Kết Quả Nhập Sách Hàng Loạt</h1>");
            pw.println("  <p>Tổng kết quá trình xử lý file dữ liệu sách</p>");
            pw.println("</header>");

            pw.println("<div style=\"display:grid; grid-template-columns: 1fr 1fr; gap:16px; margin-bottom:24px;\">");
            pw.println("  <div style=\"background:#f0fdf4; border:1px solid #bbf7d0; border-radius:12px; padding:16px; text-align:center;\">");
            pw.println("    <div style=\"font-size:2rem; font-weight:800; color:#16a34a;\">" + successCount + "</div>");
            pw.println("    <div style=\"font-size:0.9rem; font-weight:600; color:#15803d;\">Đầu sách thêm thành công</div>");
            pw.println("  </div>");
            pw.println("  <div style=\"background:" + (failureCount > 0 ? "#fef2f2" : "#f8fafc") + "; border:1px solid " + (failureCount > 0 ? "#fecaca" : "#e2e8f0") + "; border-radius:12px; padding:16px; text-align:center;\">");
            pw.println("    <div style=\"font-size:2rem; font-weight:800; color:" + (failureCount > 0 ? "#dc2626" : "#64748b") + ";\">" + failureCount + "</div>");
            pw.println("    <div style=\"font-size:0.9rem; font-weight:600; color:" + (failureCount > 0 ? "#b91c1c" : "#64748b") + ";\">Dòng lỗi / Bỏ qua</div>");
            pw.println("  </div>");
            pw.println("</div>");

            if (!errors.isEmpty()) {
                pw.println("<div style=\"background:#fff; border:1px solid #fecaca; border-radius:12px; padding:16px; margin-bottom:24px;\">");
                pw.println("  <div style=\"font-weight:700; color:#b91c1c; margin-bottom:8px;\">⚠️ Chi tiết các dòng gặp lỗi:</div>");
                pw.println("  <ul style=\"margin:0; padding-left:20px; font-size:0.875rem; color:#7f1d1d; max-height:200px; overflow-y:auto;\">");
                for (String err : errors) {
                    pw.println("    <li style=\"margin-bottom:4px;\">" + err + "</li>");
                }
                pw.println("  </ul>");
                pw.println("</div>");
            }

            pw.println("<div class='text-center' style='margin-top:20px;'>");
            pw.println("  <a href='storebooks' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary);'>&larr; Xem Kho Sách</a>");
            pw.println("  <a href='addbook' class='nav-pill-btn' style='margin-left:12px;'>+ Tiếp Tục Thêm Sách</a>");
            pw.println("</div>");

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Không thể đọc file: " + escapeHtml(e.getMessage()) + "</div>");
            showAddBookForm(pw);
        }
    }

    private static void showAddBookForm(PrintWriter pw) {
        String form = "<header class=\"bookshelf-page-header\" style=\"text-align:center;\">\r\n"
                + "      <h1>Thêm Sách Mới</h1>\r\n"
                + "      <p>Nhập thông tin chi tiết đầu sách để bổ sung vào kho hàng</p>\r\n"
                + "    </header>\r\n"
                + "    <form action=\"addbook\" method=\"post\">\r\n"
                + "      <input type=\"hidden\" name=\"action\" value=\"single\">\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookName\">Tựa sách</label>\r\n"
                + "        <input type=\"text\" name=\"name\" id=\"bookName\" placeholder=\"Ví dụ: Tôi Thấy Hoa Vàng Trên Cỏ Xanh\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookAuthor\">Tác giả</label>\r\n"
                + "        <input type=\"text\" name=\"author\" id=\"bookAuthor\" placeholder=\"Ví dụ: Nguyễn Nhật Ánh\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookPrice\">Đơn giá (VNĐ)</label>\r\n"
                + "        <input type=\"number\" name=\"price\" id=\"bookPrice\" placeholder=\"Ví dụ: 120000\" required min=\"1\" step=\"any\">\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookQuantity\">Số lượng nhập kho</label>\r\n"
                + "        <input type=\"number\" name=\"quantity\" id=\"bookQuantity\" placeholder=\"Ví dụ: 50\" required min=\"1\">\r\n"
                + "      </div>\r\n"
                + "      <button class=\"btn-pill-buy\" type=\"submit\" style=\"margin-top:12px; width:100%; padding:13px 24px; font-size:1rem; font-weight:700;\">+ Thêm Vào Kho Hàng</button>\r\n"
                + "    </form>\r\n"
                + "\r\n"
                + "    <div style=\"display:flex; align-items:center; margin: 32px 0 24px 0;\">\r\n"
                + "      <div style=\"flex:1; height:1px; background:var(--border-subtle, #e2e8f0);\"></div>\r\n"
                + "      <span style=\"padding: 0 16px; font-weight:700; color:var(--text-subtle, #64748b); font-size:0.85rem; text-transform:uppercase; letter-spacing:0.05em;\">HOẶC IMPORT TỪ FILE</span>\r\n"
                + "      <div style=\"flex:1; height:1px; background:var(--border-subtle, #e2e8f0);\"></div>\r\n"
                + "    </div>\r\n"
                + "\r\n"
                + "    <div style=\"background:var(--bg-subtle, #f8fafc); border:2px dashed var(--border-subtle, #cbd5e1); border-radius:16px; padding:24px;\">\r\n"
                + "      <header style=\"text-align:center; margin-bottom:16px;\">\r\n"
                + "        <h2 style=\"font-size:1.15rem; font-weight:700; margin-bottom:6px; color:var(--text-primary, #1e293b);\">📄 Nhập Kho Hàng Loạt từ File (.txt, .csv)</h2>\r\n"
                + "        <p style=\"font-size:0.875rem; color:var(--text-subtle, #64748b); margin:0;\">Tải file văn bản để nhập đồng thời nhiều đầu sách</p>\r\n"
                + "      </header>\r\n"
                + "      <form action=\"addbook\" method=\"post\" enctype=\"multipart/form-data\">\r\n"
                + "        <input type=\"hidden\" name=\"action\" value=\"import\">\r\n"
                + "        <div class=\"bookshelf-input-group\" style=\"margin-bottom:16px;\">\r\n"
                + "          <label for=\"importFile\" style=\"font-weight:600; display:block; margin-bottom:8px;\">Chọn file văn bản:</label>\r\n"
                + "          <input type=\"file\" name=\"importFile\" id=\"importFile\" accept=\".txt,.csv\" required \r\n"
                + "                 style=\"display:block; width:100%; padding:10px 14px; background:#fff; border:1px solid #cbd5e1; border-radius:8px; font-size:0.9rem;\">\r\n"
                + "        </div>\r\n"
                + "        <div style=\"background:#fff; border-radius:8px; border:1px solid #e2e8f0; padding:14px; margin-bottom:18px; font-size:0.825rem; color:#475569; line-height:1.6;\">\r\n"
                + "          <div style=\"font-weight:700; margin-bottom:4px; color:#1e293b;\">📌 Định dạng mỗi dòng:</div>\r\n"
                + "          <code style=\"display:block; background:#f1f5f9; padding:8px 12px; border-radius:6px; font-family:monospace; color:#0f172a; margin-bottom:6px;\">\r\n"
                + "            Tên sách, Tác giả, Đơn giá, Số lượng\r\n"
                + "          </code>\r\n"
                + "          <div style=\"color:#64748b; font-size:0.8rem;\">\r\n"
                + "            • Ví dụ: <code style=\"background:#f1f5f9; padding:2px 4px; border-radius:4px;\">Nhà Giả Kim, Paulo Coelho, 95000, 20</code><br>\r\n"
                + "            • Hỗ trợ cả dấu phân cách gạch đứng <code style=\"background:#f1f5f9; padding:2px 4px; border-radius:4px;\">|</code> nếu tên sách có dấu phẩy.<br>\r\n"
                + "            • Dòng bắt đầu bằng <code style=\"background:#f1f5f9; padding:2px 4px; border-radius:4px;\">#</code> hoặc dòng trống sẽ tự động bỏ qua.\r\n"
                + "          </div>\r\n"
                + "        </div>\r\n"
                + "        <button class=\"btn-pill-buy\" type=\"submit\" style=\"width:100%; padding:13px 24px; font-size:1rem; font-weight:700; background:linear-gradient(135deg, #2563eb, #1d4ed8); border:none; color:#fff; border-radius:10px; cursor:pointer;\">\r\n"
                + "          ⚡ Tải Lên & Nhập Kho Hàng Loạt\r\n"
                + "        </button>\r\n"
                + "      </form>\r\n"
                + "    </div>\r\n";
        pw.println(form);
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}