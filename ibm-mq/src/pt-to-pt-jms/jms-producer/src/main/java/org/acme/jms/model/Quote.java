package org.acme.jms.model;

import java.util.Random;

public class Quote {
    private static String[] skus = {"sku1", "sku2", "sku3", "sku4", "sku5", "sku6", "sku7", "sku8", "sku9", "sku10"};
    private static final Random random = new Random();

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

    public static Quote createRandomQuote(){
        return new Quote(skus[random.nextInt(skus.length)], random.nextInt(100));
    
    }
    
}