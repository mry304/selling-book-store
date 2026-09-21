package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.bittercode.model.OrderStatus;
import com.bittercode.model.UserRole;
import com.bittercode.service.OrderService;
import com.bittercode.service.impl.OrderServiceImpl;
import com.bittercode.util.StoreUtil;

public class OrderActionServlet extends HttpServlet {

    private final OrderService orderService = new OrderServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession();
        boolean isCustomer = StoreUtil.isLoggedIn(UserRole.CUSTOMER, session);
        boolean isSeller = StoreUtil.isLoggedIn(UserRole.SELLER, session);

        String action = req.getParameter("action");
        String orderId = req.getParameter("orderId");
        String reason = req.getParameter("reason");
        String newStatus = req.getParameter("status");

        // Path-based fallback: /api/orders/:id/cancel, /api/orders/:id/complete, /api/admin/orders/:id/status
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.isEmpty()) {
            String[] parts = pathInfo.split("/");
            // Example: /api/orders/ORD123/cancel -> parts[1]="orders" or orderId, etc.
            if (parts.length >= 2) {
                if (orderId == null || orderId.isEmpty()) {
                    orderId = parts[1];
                }
                if (parts.length >= 3 && (action == null || action.isEmpty())) {
                    action = parts[2];
                }
            }
        }

        boolean isJsonRequest = "json".equalsIgnoreCase(req.getParameter("format")) ||
                                (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json"));

        if (!isCustomer && !isSeller) {
            sendResponse(res, isJsonRequest, false, "Vui lòng đăng nhập để thực hiện hành động này", "CustomerLogin.html");
            return;
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            sendResponse(res, isJsonRequest, false, "Mã đơn hàng không hợp lệ", "orders");
            return;
        }

        try {
            // Customer actions
            if ("cancel".equalsIgnoreCase(action) || "customer_cancel".equalsIgnoreCase(action)) {
                if (!isCustomer && !isSeller) {
                    sendResponse(res, isJsonRequest, false, "Bạn không có quyền hủy đơn hàng này", "orders");
                    return;
                }
                if (isCustomer) {
                    String username = (String) session.getAttribute(UserRole.CUSTOMER.toString());
                    boolean ok = orderService.cancelOrderByCustomer(orderId, username, reason);
                    if (ok) {
                        sendResponse(res, isJsonRequest, true, "Hủy đơn hàng thành công và đã hoàn lại số lượng tồn kho sách!", "orders?status=CANCELLED");
                    } else {
                        sendResponse(res, isJsonRequest, false, "Không thể hủy đơn hàng này", "orders");
                    }
                    return;
                } else {
                    // Seller cancelling
                    boolean ok = orderService.updateOrderStatusBySeller(orderId, OrderStatus.CANCELLED.name(), reason);
                    if (ok) {
                        sendResponse(res, isJsonRequest, true, "Đã hủy đơn hàng và hoàn lại số lượng tồn kho!", "orders?status=CANCELLED");
                    } else {
                        sendResponse(res, isJsonRequest, false, "Không thể hủy đơn hàng này", "orders");
                    }
                    return;
                }
            } else if ("complete".equalsIgnoreCase(action) || "customer_complete".equalsIgnoreCase(action)) {
                if (isCustomer) {
                    String username = (String) session.getAttribute(UserRole.CUSTOMER.toString());
                    boolean ok = orderService.completeOrderByCustomer(orderId, username);
                    if (ok) {
                        sendResponse(res, isJsonRequest, true, "Cảm ơn bạn! Đã xác nhận nhận hàng thành công!", "orders?status=COMPLETED");
                    } else {
                        sendResponse(res, isJsonRequest, false, "Không thể xác nhận đơn hàng này", "orders");
                    }
                    return;
                } else if (isSeller) {
                    boolean ok = orderService.updateOrderStatusBySeller(orderId, OrderStatus.COMPLETED.name(), null);
                    if (ok) {
                        sendResponse(res, isJsonRequest, true, "Đã cập nhật trạng thái đơn sang Hoàn thành!", "orders?status=COMPLETED");
                    } else {
                        sendResponse(res, isJsonRequest, false, "Không thể cập nhật đơn hàng này", "orders");
                    }
                    return;
                }
            } else if ("confirm".equalsIgnoreCase(action)) {
                if (!isSeller) {
                    sendResponse(res, isJsonRequest, false, "Chỉ quản trị viên mới có quyền duyệt đơn", "orders");
                    return;
                }
                boolean ok = orderService.updateOrderStatusBySeller(orderId, OrderStatus.CONFIRMED.name(), null);
                if (ok) {
                    sendResponse(res, isJsonRequest, true, "Đã xác nhận đơn hàng thành công!", "orders?status=CONFIRMED");
                } else {
                    sendResponse(res, isJsonRequest, false, "Không thể xác nhận đơn hàng này", "orders");
                }
                return;
            } else if ("ship".equalsIgnoreCase(action)) {
                if (!isSeller) {
                    sendResponse(res, isJsonRequest, false, "Chỉ quản trị viên mới có quyền chuyển giao hàng", "orders");
                    return;
                }
                boolean ok = orderService.updateOrderStatusBySeller(orderId, OrderStatus.SHIPPING.name(), null);
                if (ok) {
                    sendResponse(res, isJsonRequest, true, "Đã chuyển đơn hàng sang trạng thái Đang giao hàng!", "orders?status=SHIPPING");
                } else {
                    sendResponse(res, isJsonRequest, false, "Không thể chuyển giao hàng cho đơn này", "orders");
                }
                return;
            } else if ("status".equalsIgnoreCase(action) || "seller_status".equalsIgnoreCase(action)) {
                if (!isSeller) {
                    sendResponse(res, isJsonRequest, false, "Chỉ quản trị viên mới có quyền đổi trạng thái đơn", "orders");
                    return;
                }
                if (newStatus == null || newStatus.trim().isEmpty()) {
                    sendResponse(res, isJsonRequest, false, "Trạng thái mới không hợp lệ", "orders");
                    return;
                }
                boolean ok = orderService.updateOrderStatusBySeller(orderId, newStatus, reason);
                if (ok) {
                    sendResponse(res, isJsonRequest, true, "Đã cập nhật trạng thái đơn hàng thành công!", "orders?status=" + newStatus);
                } else {
                    sendResponse(res, isJsonRequest, false, "Không thể cập nhật trạng thái đơn hàng", "orders");
                }
                return;
            } else {
                sendResponse(res, isJsonRequest, false, "Hành động không được hỗ trợ", "orders");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(res, isJsonRequest, false, e.getMessage(), "orders");
        }
    }

    private void sendResponse(HttpServletResponse res, boolean isJson, boolean success, String message, String redirectUrl) throws IOException {
        if (isJson) {
            res.setContentType("application/json; charset=UTF-8");
            PrintWriter pw = res.getWriter();
            String safeMsg = message != null ? message.replace("\"", "\\\"").replace("\n", " ") : "";
            pw.print("{\"success\":" + success + ",\"message\":\"" + safeMsg + "\"}");
            pw.flush();
        } else {
            res.sendRedirect(redirectUrl);
        }
    }
}
