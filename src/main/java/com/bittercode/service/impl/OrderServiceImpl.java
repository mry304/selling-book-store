package com.bittercode.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bittercode.constant.ResponseCode;
import com.bittercode.model.Cart;
import com.bittercode.model.Order;
import com.bittercode.model.OrderDetail;
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
                psOrder.setString(4, "PAID");
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
    public List<Order> getOrdersByUsername(String username) throws StoreException {
        Connection con = DBUtil.getConnection();
        List<Order> orders = new ArrayList<>();
        String validUsername = resolveUsername(con, username);
        String query = "SELECT * FROM orders WHERE username = ? ORDER BY order_date DESC";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, validUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getString("order_id"));
                    order.setUsername(rs.getString("username"));
                    order.setOrderDate(rs.getTimestamp("order_date"));
                    order.setTotalAmount(rs.getDouble("total_amount"));
                    order.setStatus(rs.getString("status"));

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
        Connection con = DBUtil.getConnection();
        List<Order> orders = new ArrayList<>();
        String query = "SELECT * FROM orders ORDER BY order_date DESC";

        try (PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Order order = new Order();
                order.setOrderId(rs.getString("order_id"));
                order.setUsername(rs.getString("username"));
                order.setOrderDate(rs.getTimestamp("order_date"));
                order.setTotalAmount(rs.getDouble("total_amount"));
                order.setStatus(rs.getString("status"));

                order.setItems(getOrderDetailsByOrderId(order.getOrderId(), con));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StoreException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return orders;
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
