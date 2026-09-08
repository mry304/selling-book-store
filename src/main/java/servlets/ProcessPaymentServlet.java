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
            StoreUtil.setActiveTab(pw, "cart");
            pw.println("<div id='topmid' style='background-color:grey'>Your Orders</div>");
            pw.println("<div class=\"container\">\r\n"
                    + "        <div class=\"card-columns\">");
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
                pw.println(this.addBookToCard(createdOrderId != null ? createdOrderId : ("ORD" + bCode), bName, bAuthor, bPrice * qtToBuy, availableQty));
                session.removeAttribute("qty_" + bCode);
            }
            session.removeAttribute("amountToPay");
            session.removeAttribute("cartItems");
            session.removeAttribute("items");
            session.removeAttribute("selectedBookId");
            pw.println("</div>\r\n"
                    + "    </div>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addBookToCard(String bCode, String bName, String bAuthor, double bPrice, int bQty) {
        String button = "<a href=\"#\" class=\"btn btn-info\">Order Placed</a>\r\n";
        return "<div class=\"card\">\r\n"
                + "                <div class=\"row card-body\">\r\n"
                + "                    <img class=\"col-sm-6\" src=\"logo.png\" alt=\"Card image cap\">\r\n"
                + "                    <div class=\"col-sm-6\">\r\n"
                + "                        <h5 class=\"card-title text-success\">" + bName + "</h5>\r\n"
                + "                        <p class=\"card-text\">\r\n"
                + "                        Author: <span class=\"text-primary\" style=\"font-weight:bold;\"> " + bAuthor
                + "</span><br>\r\n"
                + "                        </p>\r\n"
                + "                        \r\n"
                + "                    </div>\r\n"
                + "                </div>\r\n"
                + "                <div class=\"row card-body\">\r\n"
                + "                    <div class=\"col-sm-6\">\r\n"
                + "                        <p class=\"card-text\">\r\n"
                + "                        <span style='color:blue;'>Order Id: ORD" + bCode + "TM </span>\r\n"
                + "                        <br><span class=\"text-danger\">Item Yet to be Delivered</span>\r\n"
                + "                        </p>\r\n"
                + "                    </div>\r\n"
                + "                    <div class=\"col-sm-6\">\r\n"
                + "                        <p class=\"card-text\">\r\n"
                + "                        Amout Paid: <span style=\"font-weight:bold; color:green\"> &#8377; " + bPrice
                + " </span>\r\n"
                + "                        </p>\r\n"
                + button
                + "                    </div>\r\n"
                + "                </div>\r\n"
                + "            </div>";
    }
}
