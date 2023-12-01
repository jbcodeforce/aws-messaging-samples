package org.acme.orders.infra.repo;


import java.util.List;

import org.acme.orders.domain.Order;


public interface OrderRepository {
    public List<Order> getAllOrders();
    public void addOrder(Order entity);
    public void updateOrder(Order entity);
    public Order findById(String key);
}
