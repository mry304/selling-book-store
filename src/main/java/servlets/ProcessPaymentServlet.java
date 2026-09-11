package servlets;

import java.io.IOException;
import java.io.PrintWriter;
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
import com.bittercode.service.OrderService;
import com.bittercode.service.impl.BookServiceImpl;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.StoreUtil;

public class ProcessPaymentServlet extends HttpServlet {

    BookService bookService = new BookServiceImpl();

    @SuppressWarnings("unchecked")
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType(BookStoreConstants.CONTENT_TYPE_TEXT_HTML);
        if (!StoreUtil.isLoggedIn(UserRole.CUSTOMER, req.getSession())) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Please Login First to Continue!!</td></tr></table>");
            return;
        }
        try {

            RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
            rd.include(req, res);
            StoreUtil.setActiveTab(pw, "orders");

            HttpSession session = req.getSession();
            List<Cart> cartItems = null;
            if (session.getAttribute("cartItems") != null)
                cartItems = (List<Cart>) session.getAttribute("cartItems");

            String username = (String) session.getAttribute(UserRole.CUSTOMER.toString());
            if (username == null)
                username = "demo";

            double totalAmount = 0.0;
            if (cartItems != null) {
                for (Cart c : cartItems) {
                    totalAmount += c.getBook().getPrice() * c.getQuantity();
                }
            }

            OrderService orderService = new com.bittercode.service.impl.OrderServiceImpl();
            String createdOrderId = null;
            if (cartItems != null && !cartItems.isEmpty()) {
                createdOrderId = orderService.createOrder(username, cartItems, totalAmount);
            }

            pw.println("<div class=\"bookshelf-page-container\">");
            pw.println("  <div class=\"bookshelf-page-card\">");
            pw.println("    <header class=\"bookshelf-page-header\" style=\"text-align:center;\">");
            pw.println("      <div style=\"font-size:3rem; color:#16a34a; margin-bottom:12px;\">&#10003;</div>");
            pw.println("      <h1>Order Placed Successfully!</h1>");
            pw.println("      <p>Order Reference: <strong>" + (createdOrderId != null ? createdOrderId : "ORD-NEW") + "</strong> | Total Paid: <strong style='color:#2e6648;'>&#8377; " + String.format("%.2f", totalAmount) + "</strong></p>");
            pw.println("    </header>");

            pw.println("    <div class=\"table-responsive\">");
            pw.println("      <table class=\"bookshelf-table\">");
            pw.println("        <thead>");
            pw.println("          <tr>");
            pw.println("            <th>Item</th>");
            pw.println("            <th>Author</th>");
            pw.println("            <th>Quantity</th>");
            pw.println("            <th>Amount</th>");
            pw.println("            <th>Status</th>");
            pw.println("          </tr>");
            pw.println("        </thead>");
            pw.println("        <tbody>");

            if (cartItems != null) {
                for (Cart cart : cartItems) {
                    Book book = cart.getBook();
                    double bPrice = book.getPrice();
                    String bCode = book.getBarcode();
                    String bName = book.getName();
                    String bAuthor = book.getAuthor();
                    int availableQty = book.getQuantity();
                    int qtToBuy = cart.getQuantity();
                    availableQty = availableQty - qtToBuy;
                    bookService.updateBookQtyById(bCode, availableQty);

                    pw.println("          <tr>");
                    pw.println("            <td class=\"book-title-cell\">" + bName + "</td>");
                    pw.println("            <td>" + bAuthor + "</td>");
                    pw.println("            <td><strong>" + qtToBuy + "</strong></td>");
                    pw.println("            <td class=\"price-cell\">&#8377; " + String.format("%.2f", (bPrice * qtToBuy)) + "</td>");
                    pw.println("            <td><span class=\"badge-order-success\">Processing Shipment</span></td>");
                    pw.println("          </tr>");

                    session.removeAttribute("qty_" + bCode);
                }
            }

            pw.println("        </tbody>");
            pw.println("      </table>");
            pw.println("    </div>");

            pw.println("    <div style=\"display:flex; justify-content:center; gap:20px; margin-top:32px; flex-wrap:wrap;\">");
            pw.println("      <a href=\"orders\" class=\"btn-checkout-shelf\">View Order History &rarr;</a>");
            pw.println("      <a href=\"viewbook\" class=\"btn-pill-secondary\">&larr; Continue Exploring Books</a>");
            pw.println("    </div>");

            // Clear cart from session, database user_cart, and memory cache
            StoreUtil.clearUserCart(session, username);

            pw.println("  </div>");
            pw.println("</div>");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
