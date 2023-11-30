package org.acme.jms.model;

public class Quote {
    public String sku;
    public int quote;

    public Quote() {}

    public Quote(String sku, int quote) {
        this.sku = sku;
        this.quote = quote;
    }

    public String toString() {
        return "{ \"sku\": " + sku +  ",\"quote\": " + quote + " }";
    }
}