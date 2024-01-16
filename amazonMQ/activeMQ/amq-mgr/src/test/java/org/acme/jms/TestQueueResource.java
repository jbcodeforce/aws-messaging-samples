package org.acme.jms;


import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class TestQueueResource {
    
    @Test
    public void getListOfQueues()  {
       given().when().get("queues").then().statusCode(200);
    }
}
