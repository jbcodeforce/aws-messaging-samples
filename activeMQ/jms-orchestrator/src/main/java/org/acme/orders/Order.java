package org.acme.orders;

public class Order {

    public String sku;
    public double price;
    public int quantity;
    public String status;

    public Order(String sku, double price, int quantity) {
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
        this.status = "Pending";
    }

    public Order() {
    }
    
    public String toString() {
        return "{ \"sku\": " + sku +  ",\"price\": " + price + ",\"quantity\": " + quantity + ",\"status\": " + status + " }";
    }
}