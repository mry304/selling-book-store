package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.model.Book;
import com.bittercode.model.Cart;
import com.bittercode.model.UserRole;
import com.bittercode.service.BookService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.util.StoreUtil;

public class CartServlet extends HttpServlet {

    BookService bookService = new BookServiceImpl();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType("text/html; charset=UTF-8");

        // Check if Customer is logged In
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }
        try {
            // Post-Redirect-Get pattern: If POST with selectedBookId, update cart and redirect to clean GET cart
            if ("POST".equalsIgnoreCase(req.getMethod()) && req.getParameter("selectedBookId") != null) {
                StoreUtil.updateCartItems(req);
                res.sendRedirect("cart");
                return;
            }

            // Add/Remove Item from the cart if requested
            StoreUtil.updateCartItems(req);

            HttpSession session = req.getSession();
            String bookIds = "";
            if (session.getAttribute("items") != null)
                bookIds = (String) session.getAttribute("items");// read comma separated bookIds from session

            RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
            rd.include(req, res);

            // Set the active tab as cart and render cart count badge
            StoreUtil.setActiveTab(pw, "cart", session);

            // Read the books from the database with the respective bookIds
            List<Book> books = bookService.getBooksByCommaSeperatedBookIds(bookIds);
            List<Cart> cartItems = new ArrayList<Cart>();

            pw.println("<div class=\"bookshelf-page-container\">");
            pw.println("  <div class=\"bookshelf-page-card\">");
            pw.println("    <header class=\"bookshelf-page-header\">");
            pw.println("      <h1>Giỏ Hàng Của Bạn</h1>");
            pw.println("      <p>Kiểm tra các đầu sách đã chọn trước khi tiến hành thanh toán.</p>");
            pw.println("    </header>");

            double amountToPay = 0;

            if (books == null || books.isEmpty()) {
                pw.println("    <div class=\"bookshelf-empty-state\">");
                pw.println("      <div class=\"empty-icon\">&#128214;</div>");
                pw.println("      <h3>Giỏ hàng đang trống</h3>");
                pw.println("      <p>Bạn chưa thêm cuốn sách nào vào giỏ hàng của mình.</p>");
                pw.println("      <a href=\"viewbook\" class=\"btn-checkout-shelf\">&larr; Khám phá danh mục sách</a>");
                pw.println("    </div>");
            } else {
                pw.println("    <div class=\"table-responsive\">");
                pw.println("      <table class=\"bookshelf-table\">");
                pw.println("        <thead>");
                pw.println("          <tr>");
                pw.println("            <th>Mã sách</th>");
                pw.println("            <th>Tựa sách</th>");
                pw.println("            <th>Tác giả</th>");
                pw.println("            <th>Đơn giá</th>");
                pw.println("            <th style=\"text-align:center;\">Số lượng</th>");
                pw.println("            <th>Tạm tính</th>");
                pw.println("          </tr>");
                pw.println("        </thead>");
                pw.println("        <tbody>");

                for (Book book : books) {
                    int qty = 1;
                    if (session.getAttribute("qty_" + book.getBarcode()) != null) {
                        qty = (int) session.getAttribute("qty_" + book.getBarcode());
                    }
                    Cart cart = new Cart(book, qty);
                    cartItems.add(cart);
                    amountToPay += (qty * book.getPrice());
                    pw.println(getRowData(cart));
                }

                pw.println("        </tbody>");
                pw.println("      </table>");
                pw.println("    </div>"); // end table-responsive

                // Summary bar
                pw.println("    <div class=\"cart-summary-bar\">");
                pw.println("      <a href=\"viewbook\" class=\"btn-pill-secondary\">&larr; Tiếp tục mua sắm</a>");
                pw.println("      <div style=\"display:flex; align-items:center; gap:24px;\">");
                pw.println("        <div>");
                pw.println("          <span class=\"cart-total-label\">Tổng thanh toán:</span>");
                pw.println("          <span class=\"cart-total-value\">" + StoreUtil.formatPrice(amountToPay) + "</span>");
                pw.println("        </div>");
                pw.println("        <form action=\"checkout\" method=\"post\" style=\"margin:0;\">");
                pw.println("          <button type=\"submit\" class=\"btn-checkout-shelf\" name=\"pay\">Tiến hành Thanh toán &rarr;</button>");
                pw.println("        </form>");
                pw.println("      </div>");
                pw.println("    </div>");
            }

            // set cartItems and amountToPay in the session
            session.setAttribute("cartItems", cartItems);
            session.setAttribute("amountToPay", amountToPay);

            pw.println("  </div>"); // end bookshelf-page-card
            pw.println("</div>");   // end bookshelf-page-container

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRowData(Cart cart) {
        Book book = cart.getBook();
        return "    <tr>"
                + "      <td><strong>" + book.getBarcode() + "</strong></td>"
                + "      <td class=\"book-title-cell\">" + book.getName() + "</td>"
                + "      <td>" + book.getAuthor() + "</td>"
                + "      <td class=\"price-cell\">" + StoreUtil.formatPrice(book.getPrice()) + "</td>"
                + "      <td style=\"text-align:center;\">"
                + "        <form method='post' action='cart' class='cart-stepper'>"
                + "          <button type='submit' name='removeFromCart' class=\"stepper-btn minus\" title=\"Giảm một\">&minus;</button>"
                + "          <input type='hidden' name='selectedBookId' value='" + book.getBarcode() + "'/>"
                + "          <label class='stepper-quantity-field' title='Nhập số lượng rồi nhấn Enter hoặc rời khỏi ô'>"
                + "          <span>SL</span><input type='number' name='quantity' class='stepper-qty-input' value='" + cart.getQuantity() + "' min='1' max='" + book.getQuantity() + "' inputmode='numeric' aria-label='Số lượng " + escapeHtml(book.getName()) + "' onkeydown=\"if(event.key==='Enter'){event.preventDefault();this.form.requestSubmit(this.form.querySelector('.quantity-update'));}\" onblur=\"this.form.requestSubmit(this.form.querySelector('.quantity-update'));\">"
                + "          </label>"
                + "          <button type='submit' name='setQuantity' class=\"quantity-update\" hidden aria-label=\"Cập nhật số lượng\"></button>"
                + "          <button type='submit' name='addToCart' class=\"stepper-btn plus\" title=\"Tăng một\">&plus;</button>"
                + "        </form>"
                + "      </td>"
                + "      <td class=\"price-cell\"><strong>" + StoreUtil.formatPrice(book.getPrice() * cart.getQuantity()) + "</strong></td>"
                + "    </tr>";
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

}
