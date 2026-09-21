package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class StoreBookServlet extends HttpServlet {

    // book service for database operations and logics
    BookService bookService = new BookServiceImpl();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");

        // Check if the customer is logged in, or else return to login page
        if (!StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập quản trị để tiếp tục!</td></tr></table>");
            return;
        }
        try {

            RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "storebooks");

            pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:28px;\">");
            pw.println("  <div class=\"bookshelf-page-container\" style=\"max-width:1200px; margin: 0 auto;\">");
            pw.println("    <div class=\"bookshelf-page-card\" style=\"box-shadow: var(--shadow-canvas);\">");
            pw.println("      <header class=\"bookshelf-page-header\" style=\"display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px;\">");
            pw.println("        <div>");
            pw.println("          <h1>Kho Sách Cửa Hàng</h1>");
            pw.println("          <p>Quản lý toàn bộ các đầu sách đang được bày bán trong danh mục cửa hàng.</p>");
            pw.println("        </div>");
            pw.println("        <div>");
            pw.println("          <a href=\"addbook\" class=\"nav-pill-btn\" style=\"background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); font-size:0.9rem; padding:9px 20px;\">+ Thêm sách mới</a>");
            pw.println("        </div>");
            pw.println("      </header>");

            int page = getPage(req);
            final int pageSize = 12;
            int totalBooks = bookService.getBookCount();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalBooks / pageSize));
            page = Math.min(page, totalPages);
            List<Book> books = bookService.getBooksPage(page, pageSize);
            pw.println("      <div class=\"table-responsive\">");
            pw.println("        <table class=\"table table-hover align-middle\" style=\"border-radius:12px; overflow:hidden;\">");
            pw.println("          <thead style=\"background:var(--shelf-surface); color:var(--text-primary);\">");
            pw.println("            <tr>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Mã vạch (Barcode)</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Tựa sách</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Tác giả</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Đơn giá</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Số lượng tồn</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px; text-align:center;\">Thao tác</th>");
            pw.println("            </tr>");
            pw.println("          </thead>");
            pw.println("          <tbody>");

            if (books == null || books.size() == 0) {
                pw.println("            <tr><td colspan='6' class='text-center py-4' style='color:var(--text-secondary);'>Hiện chưa có sách nào trong kho lưu trữ.</td></tr>");
            } else {
                for (Book book : books) {
                    pw.println(getRowData(book));
                }
            }
            pw.println("          </tbody>");
            pw.println("        </table>");
            pw.println("      </div>");
            renderPagination(pw, page, totalPages, totalBooks);
            pw.println("    </div>");
            pw.println("  </div>");
            pw.println("</main>");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRowData(Book book) {
        return "    <tr>\r\n"
                + "      <th scope=\"row\" style=\"vertical-align:middle; font-family:monospace; font-weight:600; padding:14px;\">" + book.getBarcode() + "</th>\r\n"
                + "      <td style=\"vertical-align:middle; font-weight:600; color:var(--text-primary); padding:14px;\">" + book.getName() + "</td>\r\n"
                + "      <td style=\"vertical-align:middle; color:var(--text-secondary); padding:14px;\">" + book.getAuthor() + "</td>\r\n"
                + "      <td style=\"vertical-align:middle; font-weight:700; color:var(--accent-hover); padding:14px;\">" + StoreUtil.formatPrice(book.getPrice()) + "</td>\r\n"
                + "      <td style=\"vertical-align:middle; padding:14px;\"><span class=\"badge\" style=\"background:rgba(197,137,64,0.15); color:var(--accent-hover); font-size:0.88rem; padding:6px 12px; border-radius:999px;\">Còn " + book.getQuantity() + " cuốn</span></td>\r\n"
                + "      <td style=\"text-align:center; vertical-align:middle; padding:14px;\"><form method='post' action='updatebook' style='margin:0;'>"
                + "          <input type='hidden' name='bookId' value='" + book.getBarcode() + "'/>"
                + "          <button type='submit' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); padding:6px 18px; font-size:0.84rem; cursor:pointer;'>Cập nhật</button>"
                + "          </form></td>\r\n"
                + "    </tr>\r\n";
    }

    private int getPage(HttpServletRequest req) {
        try {
            return Math.max(1, Integer.parseInt(req.getParameter("page")));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void renderPagination(PrintWriter pw, int page, int totalPages, int totalBooks) {
        if (totalPages <= 1) return;
        pw.println("      <nav class=\"pagination-nav\" aria-label=\"Phân trang kho sách\">");
        pw.println("        <span class=\"pagination-summary\">" + totalBooks + " đầu sách · Trang " + page + "/" + totalPages + "</span>");
        pw.println("        <div class=\"pagination-links\">");
        if (page > 1) pw.println("<a href=\"storebooks?page=" + (page - 1) + "\">&larr; Trước</a>");
        for (int i = Math.max(1, page - 2); i <= Math.min(totalPages, page + 2); i++) {
            pw.println("<a class=\"" + (i == page ? "active" : "") + "\" href=\"storebooks?page=" + i + "\">" + i + "</a>");
        }
        if (page < totalPages) pw.println("<a href=\"storebooks?page=" + (page + 1) + "\">Tiếp &rarr;</a>");
        pw.println("        </div></nav>");
    }

}
