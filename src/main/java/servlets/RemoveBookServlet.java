package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.constant.ResponseCode;
import com.bittercode.model.Book;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class RemoveBookServlet extends HttpServlet {

    BookService bookService = new BookServiceImpl();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType(BookStoreConstants.CONTENT_TYPE_TEXT_HTML);
        if (!StoreUtil.isLoggedIn(UserRole.SELLER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("SellerLogin.html");
            rd.include(req, res);
            pw.println("<div class='bookshelf-auth-wrap' style='margin-top:-20px; margin-bottom:20px;'><div class='alert alert-warning text-center' style='border-radius:12px;'>Please sign in as Admin to continue!</div></div>");
            return;
        }

        try {
            String bookId = req.getParameter("bookId");
            String alertHtml = "";

            if (bookId != null && !bookId.isBlank()) {
                String responseCode = bookService.deleteBookById(bookId.trim());
                if (ResponseCode.SUCCESS.name().equalsIgnoreCase(responseCode)) {
                    alertHtml = "<div class='alert alert-success text-center' style='border-radius:12px; margin-bottom:20px;'>"
                            + "<strong>Success!</strong> The book with Barcode <code>" + bookId.trim() + "</code> has been removed from the catalog.</div>";
                } else {
                    alertHtml = "<div class='alert alert-danger text-center' style='border-radius:12px; margin-bottom:20px;'>"
                            + "<strong>Error:</strong> Failed to remove book. (It may be associated with existing orders in order history).</div>";
                }
            }

            RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "removebook");

            List<Book> books = bookService.getAllBooks();

            pw.println("<main class=\"bookshelf-canvas\" style=\"padding-top:28px;\">");
            pw.println("  <div class=\"bookshelf-page-container\" style=\"max-width:1200px; margin: 0 auto;\">");
            pw.println("    <div class=\"bookshelf-page-card\" style=\"box-shadow: var(--shadow-canvas);\">");

            if (!alertHtml.isEmpty()) {
                pw.println(alertHtml);
            }

            pw.println("      <header class=\"bookshelf-page-header\" style=\"display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:16px;\">");
            pw.println("        <div>");
            pw.println("          <h1>Remove Books</h1>");
            pw.println("          <p>Select a book from the list below to remove it from the bookstore inventory catalog.</p>");
            pw.println("        </div>");
            pw.println("        <div style=\"display:flex; gap:12px; align-items:center;\">");
            pw.println("          <input type=\"text\" id=\"bookSearchInput\" onkeyup=\"filterRemoveTable()\" placeholder=\"Search title, author, barcode...\" style=\"padding:9px 18px; border-radius:999px; border:1px solid rgba(197,137,64,0.3); font-size:0.88rem; outline:none; width:280px; background:#fff;\">");
            pw.println("          <a href=\"addbook\" class=\"nav-pill-btn\" style=\"background:var(--accent-primary); color:#fff; border-color:var(--accent-primary); font-size:0.88rem; padding:9px 18px;\">+ Add Book</a>");
            pw.println("        </div>");
            pw.println("      </header>");

            pw.println("      <div class=\"table-responsive\">");
            pw.println("        <table class=\"table table-hover align-middle\" id=\"removeBooksTable\" style=\"border-radius:12px; overflow:hidden;\">");
            pw.println("          <thead style=\"background:var(--shelf-surface); color:var(--text-primary);\">");
            pw.println("            <tr>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Barcode</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Title</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Author</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Price</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px;\">Stock Quantity</th>");
            pw.println("              <th scope=\"col\" style=\"padding:14px; text-align:center;\">Action</th>");
            pw.println("            </tr>");
            pw.println("          </thead>");
            pw.println("          <tbody>");

            if (books == null || books.isEmpty()) {
                pw.println("            <tr><td colspan='6' class='text-center py-5' style='color:var(--text-secondary);'>No books currently available in the catalog.</td></tr>");
            } else {
                for (Book book : books) {
                    pw.println(getRemoveRowData(book));
                }
            }

            pw.println("          </tbody>");
            pw.println("        </table>");
            pw.println("      </div>");

            pw.println("      <script>");
            pw.println("        function filterRemoveTable() {");
            pw.println("          var input = document.getElementById('bookSearchInput');");
            pw.println("          var filter = input.value.toLowerCase();");
            pw.println("          var table = document.getElementById('removeBooksTable');");
            pw.println("          var tr = table.getElementsByTagName('tr');");
            pw.println("          for (var i = 1; i < tr.length; i++) {");
            pw.println("            var text = tr[i].textContent || tr[i].innerText;");
            pw.println("            if (text.toLowerCase().indexOf(filter) > -1) {");
            pw.println("              tr[i].style.display = '';");
            pw.println("            } else {");
            pw.println("              tr[i].style.display = 'none';");
            pw.println("            }");
            pw.println("          }");
            pw.println("        }");
            pw.println("      </script>");

            pw.println("    </div>");
            pw.println("  </div>");
            pw.println("</main>");

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='bookshelf-auth-wrap'><div class='alert alert-danger text-center'>Error loading books list: " + e.getMessage() + "</div></div>");
        }
    }

    private String getRemoveRowData(Book book) {
        String safeName = book.getName().replace("'", "\\'").replace("\"", "&quot;");
        return "    <tr>\r\n"
                + "      <th scope=\"row\" style=\"vertical-align:middle; font-family:monospace; font-weight:600; padding:14px;\">" + book.getBarcode() + "</th>\r\n"
                + "      <td style=\"vertical-align:middle; font-weight:600; color:var(--text-primary); padding:14px;\">" + book.getName() + "</td>\r\n"
                + "      <td style=\"vertical-align:middle; color:var(--text-secondary); padding:14px;\">" + book.getAuthor() + "</td>\r\n"
                + "      <td style=\"vertical-align:middle; font-weight:700; color:var(--accent-hover); padding:14px;\">" + book.getPrice() + " đ</td>\r\n"
                + "      <td style=\"vertical-align:middle; padding:14px;\"><span class=\"badge\" style=\"background:rgba(197,137,64,0.15); color:var(--accent-hover); font-size:0.88rem; padding:6px 12px; border-radius:999px;\">" + book.getQuantity() + " in stock</span></td>\r\n"
                + "      <td style=\"text-align:center; vertical-align:middle; padding:14px;\">\r\n"
                + "        <form method='post' action='removebook' style='margin:0;' onsubmit=\"return confirm('Are you sure you want to permanently remove \\'" + safeName + "\\' (Barcode: " + book.getBarcode() + ")?');\">\r\n"
                + "          <input type='hidden' name='bookId' value='" + book.getBarcode() + "'/>\r\n"
                + "          <button type='submit' class='nav-pill-btn' style='background:#ef4444; color:#fff; border-color:#ef4444; padding:6px 18px; font-size:0.84rem; cursor:pointer; box-shadow:0 2px 6px rgba(239,68,68,0.25); transition:all 0.2s;'>&#128465; Remove</button>\r\n"
                + "        </form>\r\n"
                + "      </td>\r\n"
                + "    </tr>\r\n";
    }
}
