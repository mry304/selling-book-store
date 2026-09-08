package com.bittercode.service;

import java.util.List;
import com.bittercode.model.Cart;
import com.bittercode.model.Order;
import com.bittercode.model.StoreException;

public interface OrderService {

    public String createOrder(String username, List<Cart> cartItems, double totalAmount) throws StoreException;

    public List<Order> getOrdersByUsername(String username) throws StoreException;

    public List<Order> getAllOrders() throws StoreException;
}
