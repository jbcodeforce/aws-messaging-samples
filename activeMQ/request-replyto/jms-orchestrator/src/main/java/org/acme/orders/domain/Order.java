package org.acme.orders.domain;
import java.util.Random;

import org.jgroups.util.UUID;
public class Order {

    public static final String PENDING_STATUS = "pending";
    public static final String CANCELLED_STATUS = "cancelled";
    public static final String ONHOLD_STATUS = "onHold";
    public static final String ASSIGNED_STATUS = "assigned";
    public static final String REJECTED_STATUS = "rejected";
    public static final String COMPLETED_STATUS = "completed";
    
    private final static Random random = new Random();
    private static String[] skus = {"sku1", "sku2", "sku3", "sku4", "sku5", "sku6", "sku7", "sku8", "sku9", "sku10"};
   
    public String orderID;
    public String sku;
    public double price;
    public int quantity;
    public String status;
    public String updateDate;
    public String creationDate;

    public Order(String oid,String sku, double price, int quantity) {
        this.sku = sku;
        this.orderID = oid;
        this.price = price;
        this.quantity = quantity;
        this.status = "Pending";
    }

    public Order() {
    }
    
    public String toString() {
        return "{ \"sku\": " + sku +  ",\"price\": " + price + ",\"quantity\": " + quantity + ",\"status\": " + status + " }";
    }

    public static Order buildOrder(String oid) {
        int q = random.nextInt(10);
        
        double p = random.nextDouble(600) * q;

        return new Order(oid,skus[random.nextInt(skus.length)], p, q);  
        
    }

    public static Order buildOrder() {
        return buildOrder(UUID.randomUUID().toString());
    }
    

}