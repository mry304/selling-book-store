package com.bittercode.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.bittercode.constant.ResponseCode;
import com.bittercode.model.Cart;
import com.bittercode.model.Order;
import com.bittercode.model.OrderDetail;
import com.bittercode.model.OrderStatus;
import com.bittercode.model.StoreException;
import com.bittercode.service.OrderService;
import com.bittercode.util.DBUtil;

public class OrderServiceImpl implements OrderService {

    @Override
    public String createOrder(String username, List<Cart> cartItems, double totalAmount) throws StoreException {
        Connection con = DBUtil.getConnection();
        String orderId = "ORD" + System.currentTimeMillis();
        String validUsername = resolveUsername(con, username);

        String insertOrderQuery = "INSERT INTO orders (order_id, username, total_amount, status) VALUES (?, ?, ?, ?)";
        String insertDetailQuery = "INSERT INTO order_details (order_id, book_barcode, quantity, amount) VALUES (?, ?, ?, ?)";

        try {
            con.setAutoCommit(false);

            try (PreparedStatement psOrder = con.prepareStatement(insertOrderQuery)) {
                psOrder.setString(1, orderId);
                psOrder.setString(2, validUsername);
                psOrder.setDouble(3, totalAmount);
                psOrder.setString(4, OrderStatus.PENDING.name());
                psOrder.executeUpdate();
            }

            try (PreparedStatement psDetail = con.prepareStatement(insertDetailQuery)) {
                for (Cart cart : cartItems) {
                    psDetail.setString(1, orderId);
                    psDetail.setString(2, cart.getBook().getBarcode());
                    psDetail.setInt(3, cart.getQuantity());
                    psDetail.setDouble(4, cart.getBook().getPrice() * cart.getQuantity());
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            con.commit();
            return orderId;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Order getOrderById(String orderId) throws StoreException {
        Connection con = DBUtil.getConnection();
        String query = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderDetailsByOrderId(orderId, con));
                    return order;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    @Override
    public List<Order> getOrdersByUsername(String username) throws StoreException {
        return getOrdersByUsername(username, null);
    }

    @Override
    public List<Order> getOrdersByUsername(String username, String statusFilter) throws StoreException {
        checkAndAutoCompleteOrders();

        Connection con = DBUtil.getConnection();
        List<Order> orders = new ArrayList<>();
        String validUsername = resolveUsername(con, username);

        StringBuilder query = new StringBuilder("SELECT * FROM orders WHERE username = ? ");
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            query.append("AND status = ? ");
        }
        query.append("ORDER BY order_date DESC");

        try (PreparedStatement ps = con.prepareStatement(query.toString())) {
            ps.setString(1, validUsername);
            if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(2, statusFilter.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderDetailsByOrderId(order.getOrderId(), con));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return orders;
    }

    @Override
    public List<Order> getAllOrders() throws StoreException {
        return getAllOrders(null);
    }

    @Override
    public List<Order> getAllOrders(String statusFilter) throws StoreException {
        checkAndAutoCompleteOrders();

        Connection con = DBUtil.getConnection();
        List<Order> orders = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM orders ");
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            query.append("WHERE status = ? ");
        }
        query.append("ORDER BY order_date DESC");

        try (PreparedStatement ps = con.prepareStatement(query.toString())) {
            if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(1, statusFilter.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getOrderDetailsByOrderId(order.getOrderId(), con));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return orders;
    }

    @Override
    public boolean cancelOrderByCustomer(String orderId, String username, String cancelReason) throws StoreException {
        Connection con = DBUtil.getConnection();
        String validUsername = resolveUsername(con, username);

        // Verify order exists, belongs to customer and is in PENDING state
        Order order = getOrderById(orderId);
        if (order == null) {
            throw new StoreException("Đơn hàng không tồn tại");
        }
        if (!order.getUsername().equalsIgnoreCase(validUsername)) {
            throw new StoreException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        if (!OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            throw new StoreException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái 'Chờ xác nhận'");
        }

        String updateOrderSql = "UPDATE orders SET status = 'CANCELLED', cancelled_by = 'CUSTOMER', " +
                                "cancel_reason = ?, cancelled_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status = 'PENDING'";
        String restoreStockSql = "UPDATE books SET quantity = quantity + ? WHERE barcode = ?";

        try {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(updateOrderSql)) {
                ps.setString(1, cancelReason != null && !cancelReason.trim().isEmpty() ? cancelReason.trim() : "Khách hàng hủy đơn");
                ps.setString(2, orderId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    con.rollback();
                    return false;
                }
            }

            // Restore book inventory
            List<OrderDetail> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                try (PreparedStatement psStock = con.prepareStatement(restoreStockSql)) {
                    for (OrderDetail item : items) {
                        psStock.setInt(1, item.getQuantity());
                        psStock.setString(2, item.getBookBarcode());
                        psStock.addBatch();
                    }
                    psStock.executeBatch();
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean completeOrderByCustomer(String orderId, String username) throws StoreException {
        Connection con = DBUtil.getConnection();
        String validUsername = resolveUsername(con, username);

        Order order = getOrderById(orderId);
        if (order == null) {
            throw new StoreException("Đơn hàng không tồn tại");
        }
        if (!order.getUsername().equalsIgnoreCase(validUsername)) {
            throw new StoreException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        if (!OrderStatus.SHIPPING.name().equalsIgnoreCase(order.getStatus())) {
            throw new StoreException("Chỉ có thể xác nhận nhận hàng khi đơn hàng đang ở trạng thái 'Đang giao hàng'");
        }

        String sql = "UPDATE orders SET status = 'COMPLETED' WHERE order_id = ? AND status = 'SHIPPING'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean updateOrderStatusBySeller(String orderId, String newStatus, String cancelReason) throws StoreException {
        if (newStatus == null) return false;
        newStatus = newStatus.trim().toUpperCase();

        Order order = getOrderById(orderId);
        if (order == null) {
            throw new StoreException("Đơn hàng không tồn tại");
        }

        Connection con = DBUtil.getConnection();

        if (OrderStatus.CONFIRMED.name().equals(newStatus)) {
            if (!OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
                throw new StoreException("Chỉ có thể duyệt đơn khi đơn đang ở trạng thái 'Chờ xác nhận'");
            }
            String sql = "UPDATE orders SET status = 'CONFIRMED' WHERE order_id = ? AND status = 'PENDING'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, orderId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else if (OrderStatus.SHIPPING.name().equals(newStatus)) {
            if (!OrderStatus.CONFIRMED.name().equalsIgnoreCase(order.getStatus())) {
                throw new StoreException("Chỉ có thể giao hàng cho đơn đã xác nhận");
            }
            String sql = "UPDATE orders SET status = 'SHIPPING', shipped_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status = 'CONFIRMED'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, orderId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else if (OrderStatus.COMPLETED.name().equals(newStatus)) {
            if (!OrderStatus.SHIPPING.name().equalsIgnoreCase(order.getStatus())) {
                throw new StoreException("Chỉ có thể hoàn thành khi đơn hàng đang giao");
            }
            String sql = "UPDATE orders SET status = 'COMPLETED' WHERE order_id = ? AND status = 'SHIPPING'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, orderId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else if (OrderStatus.CANCELLED.name().equals(newStatus)) {
            if (!OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus()) && !OrderStatus.CONFIRMED.name().equalsIgnoreCase(order.getStatus())) {
                throw new StoreException("Người bán chỉ có thể hủy đơn khi đơn ở trạng thái 'Chờ xác nhận' hoặc 'Đã xác nhận'");
            }

            String updateOrderSql = "UPDATE orders SET status = 'CANCELLED', cancelled_by = 'SELLER', " +
                                    "cancel_reason = ?, cancelled_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status IN ('PENDING', 'CONFIRMED')";
            String restoreStockSql = "UPDATE books SET quantity = quantity + ? WHERE barcode = ?";

            try {
                con.setAutoCommit(false);

                try (PreparedStatement ps = con.prepareStatement(updateOrderSql)) {
                    ps.setString(1, cancelReason != null && !cancelReason.trim().isEmpty() ? cancelReason.trim() : "Người bán hủy đơn");
                    ps.setString(2, orderId);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        con.rollback();
                        return false;
                    }
                }

                // Restore book inventory
                List<OrderDetail> items = order.getItems();
                if (items != null && !items.isEmpty()) {
                    try (PreparedStatement psStock = con.prepareStatement(restoreStockSql)) {
                        for (OrderDetail item : items) {
                            psStock.setInt(1, item.getQuantity());
                            psStock.setString(2, item.getBookBarcode());
                            psStock.addBatch();
                        }
                        psStock.executeBatch();
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new StoreException("Trạng thái không hợp lệ: " + newStatus);
        }
    }

    @Override
    public int checkAndAutoCompleteOrders() throws StoreException {
        // Auto-complete orders that have been in SHIPPING status for more than 3 days
        String sql = "UPDATE orders SET status = 'COMPLETED' " +
                     "WHERE status = 'SHIPPING' AND shipped_at IS NOT NULL " +
                     "AND shipped_at <= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY)";
        try {
            Connection con = DBUtil.getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Log and do not break caller flow
            e.printStackTrace();
            return 0;
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getString("order_id"));
        order.setUsername(rs.getString("username"));
        order.setOrderDate(rs.getTimestamp("order_date"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(rs.getString("status"));

        try {
            order.setCancelReason(rs.getString("cancel_reason"));
        } catch (SQLException ignored) {}

        try {
            order.setCancelledBy(rs.getString("cancelled_by"));
        } catch (SQLException ignored) {}

        try {
            order.setCancelledAt(rs.getTimestamp("cancelled_at"));
        } catch (SQLException ignored) {}

        try {
            order.setShippedAt(rs.getTimestamp("shipped_at"));
        } catch (SQLException ignored) {}

        return order;
    }

    private List<OrderDetail> getOrderDetailsByOrderId(String orderId, Connection con) throws SQLException {
        List<OrderDetail> details = new ArrayList<>();
        String query = "SELECT od.*, b.name AS book_name FROM order_details od " +
                       "LEFT JOIN books b ON od.book_barcode = b.barcode " +
                       "WHERE od.order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail detail = new OrderDetail();
                    detail.setId(rs.getInt("id"));
                    detail.setOrderId(rs.getString("order_id"));
                    detail.setBookBarcode(rs.getString("book_barcode"));
                    detail.setBookName(rs.getString("book_name"));
                    detail.setQuantity(rs.getInt("quantity"));
                    detail.setAmount(rs.getDouble("amount"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    private String resolveUsername(Connection con, String username) {
        if (username == null || username.trim().isEmpty()) {
            return username;
        }
        String query = "SELECT username FROM users WHERE username = ? OR mailid = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return username;
    }
}
