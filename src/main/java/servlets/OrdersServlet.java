package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.constant.BookStoreConstants;
import com.bittercode.model.Order;
import com.bittercode.model.OrderDetail;
import com.bittercode.model.UserRole;
import com.bittercode.service.OrderService;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.StoreUtil;

public class OrdersServlet extends HttpServlet {

    private OrderService orderService = new OrderServiceImpl();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        res.setContentType(BookStoreConstants.CONTENT_TYPE_TEXT_HTML);

        HttpSession session = req.getSession();
        boolean isCustomer = StoreUtil.isLoggedIn(UserRole.CUSTOMER, session);
        boolean isSeller = StoreUtil.isLoggedIn(UserRole.SELLER, session);

        if (!isCustomer && !isSeller) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Please Login First to Continue!!</td></tr></table>");
            return;
        }

        try {
            if (isCustomer) {
                RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders");
            } else {
                RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders");
            }

            String username = isCustomer ? (String) session.getAttribute(UserRole.CUSTOMER.toString()) : null;
            List<Order> orders = isCustomer ? orderService.getOrdersByUsername(username) : orderService.getAllOrders();

            pw.println("<div id='topmid' style='background-color: #343a40; color: white; padding: 10px; text-align: center; margin-bottom: 20px;'>");
            pw.println("<h2>" + (isCustomer ? "My Order History" : "Customer Orders Management") + "</h2>");
            pw.println("</div>");

            pw.println("<div class='container' style='margin-top: 20px;'>");

            if (orders == null || orders.isEmpty()) {
                pw.println("<div class='alert alert-info text-center'>No orders found!</div>");
            } else {
                pw.println("<div class='table-responsive'>");
                pw.println("<table class='table table-bordered table-striped table-hover'>");
                pw.println("<thead class='thead-dark'>");
                pw.println("<tr>");
                pw.println("<th>Order ID</th>");
                if (isSeller) {
                    pw.println("<th>Customer</th>");
                }
                pw.println("<th>Order Date</th>");
                pw.println("<th>Items Purchased</th>");
                pw.println("<th>Total Amount</th>");
                pw.println("<th>Status</th>");
                pw.println("</tr>");
                pw.println("</thead>");
                pw.println("<tbody>");

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

                for (Order order : orders) {
                    pw.println("<tr>");
                    pw.println("<td><strong>" + order.getOrderId() + "</strong></td>");
                    if (isSeller) {
                        pw.println("<td>" + order.getUsername() + "</td>");
                    }
                    pw.println("<td>" + (order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "N/A") + "</td>");

                    // Items list
                    pw.println("<td><ul style='margin-bottom: 0; padding-left: 20px;'>");
                    for (OrderDetail item : order.getItems()) {
                        String name = item.getBookName() != null ? item.getBookName() : ("Book (" + item.getBookBarcode() + ")");
                        pw.println("<li>" + name + " x <strong>" + item.getQuantity() + "</strong> (&#8377; " + item.getAmount() + ")</li>");
                    }
                    pw.println("</ul></td>");

                    pw.println("<td style='color: green; font-weight: bold;'>&#8377; " + order.getTotalAmount() + "</td>");
                    pw.println("<td><span class='badge badge-success' style='font-size: 14px;'>" + order.getStatus() + "</span></td>");
                    pw.println("</tr>");
                }

                pw.println("</tbody>");
                pw.println("</table>");
                pw.println("</div>");
            }

            pw.println("</div>");

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='container my-4'><div class='alert alert-danger'>Error loading orders: " + e.getMessage() + "</div></div>");
        }
    }
}
