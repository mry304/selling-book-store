package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.constant.db.BooksDBConstants;
import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class AddBookServlet extends HttpServlet {
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

        String bName = req.getParameter(BooksDBConstants.COLUMN_NAME);
        RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
        rd.include(req, res);
        StoreUtil.setActiveTab(pw, "addbook");
        pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:28px;\">");
        pw.println("  <div class=\"bookshelf-page-container\" style=\"max-width:640px; margin: 0 auto;\">");
        pw.println("    <div class=\"bookshelf-page-card\" style=\"box-shadow: var(--shadow-canvas);\">");
        if(bName == null || bName.isBlank()) {
            showAddBookForm(pw);
            pw.println("    </div></div></main>");
            return;
        }
        try {
            String uniqueID = UUID.randomUUID().toString();
            String bCode = uniqueID;
            String bAuthor = req.getParameter(BooksDBConstants.COLUMN_AUTHOR);
            double bPrice = Double.parseDouble(req.getParameter(BooksDBConstants.COLUMN_PRICE));
            int bQty = Integer.parseInt(req.getParameter(BooksDBConstants.COLUMN_QUANTITY));

            Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
            String message = bookService.addBook(book);
            if ("SUCCESS".equalsIgnoreCase(message)) {
                pw.println("<div class='alert alert-success text-center' style='border-radius:12px; margin-bottom:24px;'>Đã thêm sách \"" + bName + "\" vào kho thành công!</div>");
                pw.println("<div class='text-center'><a href='storebooks' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary);'>&larr; Xem Kho Sách</a> <a href='addbook' class='nav-pill-btn' style='margin-left:12px;'>+ Thêm Cuốn Sách Khác</a></div>");
            } else {
                pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Thêm sách thất bại. Vui lòng kiểm tra lại các trường thông tin!</div>");
                showAddBookForm(pw);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Đã xảy ra lỗi khi thêm sách. Vui lòng thử lại!</div>");
            showAddBookForm(pw);
        }
        pw.println("    </div></div></main>");
    }
    
    private static void showAddBookForm(PrintWriter pw) {
        String form = "<header class=\"bookshelf-page-header\" style=\"text-align:center;\">\r\n"
                + "      <h1>Thêm Sách Mới</h1>\r\n"
                + "      <p>Nhập thông tin chi tiết đầu sách để bổ sung vào kho hàng</p>\r\n"
                + "    </header>\r\n"
                + "    <form action=\"addbook\" method=\"post\">\r\n"
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
                + "    </form>\r\n";
        pw.println(form);
    }
}
