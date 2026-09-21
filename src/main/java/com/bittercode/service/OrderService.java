package com.bittercode.service;

import java.util.List;
import com.bittercode.model.Cart;
import com.bittercode.model.Order;
import com.bittercode.model.StoreException;

public interface OrderService {

    public String createOrder(String username, List<Cart> cartItems, double totalAmount) throws StoreException;

    public String createOrder(String username, List<Cart> cartItems, double totalAmount,
                              String shippingEmail, String shippingAddress, String paymentMethod) throws StoreException;

    public Order getOrderById(String orderId) throws StoreException;

    public List<Order> getOrdersByUsername(String username) throws StoreException;

    public List<Order> getOrdersByUsername(String username, String statusFilter) throws StoreException;

    public List<Order> getAllOrders() throws StoreException;

    public List<Order> getAllOrders(String statusFilter) throws StoreException;

    public boolean cancelOrderByCustomer(String orderId, String username, String cancelReason) throws StoreException;

    public boolean completeOrderByCustomer(String orderId, String username) throws StoreException;

    public boolean updateOrderStatusBySeller(String orderId, String newStatus, String cancelReason) throws StoreException;

    public int checkAndAutoCompleteOrders() throws StoreException;
}
