package org.acme.orders.infra.msg;

import java.util.UUID;

import org.acme.orders.domain.Order;


public class OrderMessage {
    
    public String messageID;
    public String orderID;
    public String sku;
    public double price;
    public int quantity;
    public String status;

    public OrderMessage() {
    }

    public static OrderMessage fromOrder(Order order) {
        OrderMessage oe = new OrderMessage();
        oe.messageID = UUID.randomUUID().toString();
        oe.orderID = order.orderID;
        oe.sku = order.sku;
        oe.price = order.price;
        oe.quantity = order.quantity;
        oe.status = order.status;
        return oe;
    }

    public String toString(){
        return messageID + "," + orderID + "," + sku + "," + price + "," + quantity + "," + status;
    }

    public static Order toOrder(OrderMessage om) {
        Order o = new Order(om.orderID, om.sku, om.price, om.quantity);
        o.status = om.status;
        return o;
    }
}
