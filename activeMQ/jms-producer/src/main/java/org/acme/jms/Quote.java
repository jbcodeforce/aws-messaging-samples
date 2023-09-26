package org.acme.jms;

public class Quote {
    public String sku;
    public int quote;

    public Quote(String sku, int quote) {
        this.sku = sku;
        this.quote = quote;
    }

    public String toString() {
        return "{ \"sku\": " + sku +  ",\"quote\": " + quote + " }";
    }
}