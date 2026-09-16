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
        res.setContentType(BookStoreConstants.CONTENT_TYPE_TEXT_HTML);

        if (!StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Please Login First to Continue!!</td></tr></table>");
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
            double bPrice = Integer.parseInt(req.getParameter(BooksDBConstants.COLUMN_PRICE));
            int bQty = Integer.parseInt(req.getParameter(BooksDBConstants.COLUMN_QUANTITY));

            Book book = new Book(bCode, bName, bAuthor, bPrice, bQty);
            String message = bookService.addBook(book);
            if ("SUCCESS".equalsIgnoreCase(message)) {
                pw.println("<div class='alert alert-success text-center' style='border-radius:12px; margin-bottom:24px;'>Book \"" + bName + "\" added successfully to store catalog!</div>");
                pw.println("<div class='text-center'><a href='storebooks' class='nav-pill-btn' style='background:var(--accent-primary); color:#fff; border-color:var(--accent-primary);'>&larr; View Store Inventory</a> <a href='addbook' class='nav-pill-btn' style='margin-left:12px;'>+ Add Another Book</a></div>");
            } else {
                pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>Failed to add book. Please fill up all fields carefully!</div>");
                showAddBookForm(pw);
            }
        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:24px;'>An error occurred while adding the book. Please try again!</div>");
            showAddBookForm(pw);
        }
        pw.println("    </div></div></main>");
    }
    
    private static void showAddBookForm(PrintWriter pw) {
        String form = "<header class=\"bookshelf-page-header\" style=\"text-align:center;\">\r\n"
                + "      <h1>Add New Book</h1>\r\n"
                + "      <p>Enter details of the title to add to your bookstore collection</p>\r\n"
                + "    </header>\r\n"
                + "    <form action=\"addbook\" method=\"post\">\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookName\">Book Title</label>\r\n"
                + "        <input type=\"text\" name=\"name\" id=\"bookName\" placeholder=\"e.g. Clean Architecture\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookAuthor\">Author</label>\r\n"
                + "        <input type=\"text\" name=\"author\" id=\"bookAuthor\" placeholder=\"e.g. Robert C. Martin\" required>\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookPrice\">Price (đ)</label>\r\n"
                + "        <input type=\"number\" name=\"price\" id=\"bookPrice\" placeholder=\"e.g. 299\" required min=\"1\">\r\n"
                + "      </div>\r\n"
                + "      <div class=\"bookshelf-input-group\">\r\n"
                + "        <label for=\"bookQuantity\">Stock Quantity</label>\r\n"
                + "        <input type=\"number\" name=\"quantity\" id=\"bookQuantity\" placeholder=\"e.g. 25\" required min=\"1\">\r\n"
                + "      </div>\r\n"
                + "      <button class=\"btn-auth-submit\" type=\"submit\" style=\"margin-top:12px;\">+ Add to Catalog</button>\r\n"
                + "    </form>\r\n";
        pw.println(form);
    }
}
