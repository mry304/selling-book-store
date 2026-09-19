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
        res.setContentType("text/html; charset=UTF-8");

        HttpSession session = req.getSession();
        boolean isCustomer = StoreUtil.isLoggedIn(UserRole.CUSTOMER, session);
        boolean isSeller = StoreUtil.isLoggedIn(UserRole.SELLER, session);

        if (!isCustomer && !isSeller) {
            RequestDispatcher rd = req.getRequestDispatcher("CustomerLogin.html");
            rd.include(req, res);
            pw.println("<table class=\"tab\"><tr><td>Vui lòng đăng nhập để tiếp tục!</td></tr></table>");
            return;
        }

        try {
            if (isCustomer) {
                RequestDispatcher rd = req.getRequestDispatcher("CustomerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders", session);
            } else {
                RequestDispatcher rd = req.getRequestDispatcher("SellerHome.html");
                rd.include(req, res);
                StoreUtil.setActiveTab(pw, "orders");
            }

            String username = isCustomer ? (String) session.getAttribute(UserRole.CUSTOMER.toString()) : null;
            List<Order> orders = isCustomer ? orderService.getOrdersByUsername(username) : orderService.getAllOrders();

            pw.println("<div class=\"bookshelf-page-container\">");
            pw.println("  <div class=\"bookshelf-page-card\">");
            pw.println("    <header class=\"bookshelf-page-header\">");
            pw.println("      <h1>" + (isCustomer ? "Lịch Sử Đơn Hàng Của Tôi" : "Quản Lý Đơn Đặt Hàng") + "</h1>");
            pw.println("      <p>" + (isCustomer ? "Xem lại các tựa sách đã mua và tiến độ giao hàng." : "Quản lý toàn bộ danh sách đơn đặt hàng và doanh thu từ khách hàng.") + "</p>");
            pw.println("    </header>");

            if (orders == null || orders.isEmpty()) {
                pw.println("    <div class=\"bookshelf-empty-state\">");
                pw.println("      <div class=\"empty-icon\">&#128220;</div>");
                pw.println("      <h3>Chưa có đơn hàng nào</h3>");
                pw.println("      <p>Khi bạn đặt mua sách, biên lai và trạng thái đơn hàng sẽ hiển thị tại đây.</p>");
                pw.println("      <a href=\"viewbook\" class=\"btn-checkout-shelf\">&larr; Khám phá danh mục sách</a>");
                pw.println("    </div>");
            } else {
                pw.println("    <div class=\"table-responsive\">");
                pw.println("      <table class=\"bookshelf-table\">");
                pw.println("        <thead>");
                pw.println("          <tr>");
                pw.println("            <th>Mã đơn</th>");
                if (isSeller) {
                    pw.println("            <th>Khách hàng</th>");
                }
                pw.println("            <th>Thời gian</th>");
                pw.println("            <th>Sách đã mua</th>");
                pw.println("            <th>Tổng thanh toán</th>");
                pw.println("            <th>Trạng thái</th>");
                pw.println("          </tr>");
                pw.println("        </thead>");
                pw.println("        <tbody>");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                for (Order order : orders) {
                    pw.println("          <tr>");
                    pw.println("            <td><strong>" + order.getOrderId() + "</strong></td>");
                    if (isSeller) {
                        pw.println("            <td>" + order.getUsername() + "</td>");
                    }
                    pw.println("            <td style=\"color:var(--text-secondary);\">" + (order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "N/A") + "</td>");

                    // Items list
                    pw.println("            <td><ul style=\"margin:0; padding-left:18px; line-height:1.6;\">");
                    for (OrderDetail item : order.getItems()) {
                        String name = item.getBookName() != null ? item.getBookName() : ("Sách (" + item.getBookBarcode() + ")");
                        pw.println("              <li><span class=\"book-title-cell\" style=\"font-size:0.95rem;\">" + name + "</span> &times; <strong>" + item.getQuantity() + "</strong> (" + StoreUtil.formatPrice(item.getAmount()) + ")</li>");
                    }
                    pw.println("            </ul></td>");

                    String st = order.getStatus();
                    String stVi = "ĐÃ THANH TOÁN";
                    if ("PROCESSING".equalsIgnoreCase(st)) stVi = "ĐANG XỬ LÝ";
                    else if ("SHIPPED".equalsIgnoreCase(st)) stVi = "ĐÃ GIAO HÀNG";
                    else if ("CANCELLED".equalsIgnoreCase(st)) stVi = "ĐÃ HỦY";

                    pw.println("            <td class=\"price-cell\">" + StoreUtil.formatPrice(order.getTotalAmount()) + "</td>");
                    pw.println("            <td><span class=\"badge-order-success\">" + stVi + "</span></td>");
                    pw.println("          </tr>");
                }

                pw.println("        </tbody>");
                pw.println("      </table>");
                pw.println("    </div>"); // end table-responsive
            }

            pw.println("  </div>"); // end bookshelf-page-card
            pw.println("</div>");   // end bookshelf-page-container

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("<div class='container my-4'><div class='alert alert-danger'>Error loading orders: " + e.getMessage() + "</div></div>");
        }
    }
}
