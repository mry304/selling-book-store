package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.constant.ResponseCode;
import com.bittercode.constant.db.BooksDBConstants;
import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class UpdateBookServlet extends HttpServlet {
    BookService bookService = new BookServiceImpl();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");

        if (!StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập quản trị để tiếp tục!</td></tr></table>");
            return;
        }

        RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
        rd.include(req, res);
        StoreUtil.setActiveTab(pw, "storebooks");
        pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:28px;\">");
        pw.println("  <div class=\"bookshelf-page-container\" style=\"max-width:640px; margin: 0 auto;\">");
        pw.println("    <div class=\"bookshelf-page-card\" style=\"box-shadow: var(--shadow-canvas);\">");

        try {
            if (req.getParameter("updateFormSubmitted") != null) {
                String bName = req.getParameter(BooksDBConstants.COLUMN_NAME);
                String bCode = req.getParameter(BooksDBConstants.COLUMN_BARCODE);
                String bAuthor = req.getParameter(BooksDBConstants.COLUMN_AUTHOR);
                double bPrice = Double.parseDouble(req.getParameter(BooksDBConstants.COLUMN_PRICE));
                int bQty = Integer.parseInt(req.getParameter(BooksDBConstants.COLUMN_QUANTITY));

                Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
                String message = bookService.updateBook(book);
                if (ResponseCode.SUCCESS.name().equalsIgnoreCase(message)) {
                    pw.println("<div class='alert alert-success text-center' style='border-radius:12px; margin-bottom:20px;'>Đã cập nhật thông tin sách \"" + bName + "\" thành công!</div>");
                } else {
                    pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:20px;'>Cập nhật thất bại. Vui lòng thử lại!</div>");
                }
                pw.println("<div class='text-center mt-3'><a href='storebooks' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary);'>&larr; Quay Lại Kho Sách</a></div>");
                pw.println("    </div></div></main>");
                return;
            }

            String bookId = req.getParameter("bookId");

            if (bookId != null) {
                Book book = bookService.getBookById(bookId);
                showUpdateBookForm(pw, book);
            }

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px;'>Tải thông tin sách thất bại!</div>");
        }
        pw.println("    </div></div></main>");
    }

    private static void showUpdateBookForm(PrintWriter pw, Book book) {
        String form = "<header class=\"bookshelf-page-header\" style=\"text-align:center;\">\r\n"
                + "      <h1>Cập Nhật Thông Tin Sách</h1>\r\n"
                + "      <p>Chỉnh sửa chi tiết đầu sách và điều chỉnh số lượng tồn kho</p>\r\n"
                + "    </header>\r\n"
                + "    <form action=\"updatebook\" method=\"post\">\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookCode\">Mã vạch (Barcode - Chỉ đọc)</label>\r\n"
                + "        <input type=\"text\" name=\"barcode\" id=\"bookCode\" value=\"" + book.getBarcode() + "\" readonly style=\"background:var(--bg-board); cursor:not-allowed;\">\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookName\">Tựa sách</label>\r\n"
                + "        <input type=\"text\" name=\"name\" id=\"bookName\" value=\"" + book.getName() + "\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookAuthor\">Tác giả</label>\r\n"
                + "        <input type=\"text\" name=\"author\" id=\"bookAuthor\" value=\"" + book.getAuthor() + "\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookPrice\">Đơn giá (VNĐ)</label>\r\n"
                + "        <input type=\"number\" name=\"price\" id=\"bookPrice\" value=\"" + (long)book.getPrice() + "\" required min=\"1\" step=\"any\">\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookQuantity\">Số lượng tồn kho</label>\r\n"
                + "        <input type=\"number\" name=\"quantity\" id=\"bookQuantity\" value=\"" + book.getQuantity() + "\" required min=\"0\">\r\n"
                + "      </div>\r\n"
                + "      <button class=\"btn-auth-submit\" type=\"submit\" name=\"updateFormSubmitted\" value=\"true\" style=\"margin-top:12px;\">Lưu Thay Đổi</button>\r\n"
                + "      <div style=\"text-align:center; margin-top:16px;\"><a href=\"storebooks\" style=\"color:var(--text-secondary); text-decoration:none; font-size:0.9rem;\">&larr; Hủy bỏ và quay lại kho sách</a></div>\r\n"
                + "    </form>\r\n";
        pw.println(form);
    }
}
