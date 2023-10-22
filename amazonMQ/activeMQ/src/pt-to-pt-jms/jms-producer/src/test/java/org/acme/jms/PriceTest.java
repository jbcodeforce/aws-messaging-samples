package org.acme.jms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(ArtemisTestResource.class)
public class PriceTest {

    @Test
    public void testLastPrice() {
       
        String requestBody = "{\n " +
        "\"messageToSend\": 10,\n" +
        "\"delay\": 0\n" +
        "}";
        Response response = given()
                .header("Content-type", "application/json")
                .and()
                .body(requestBody)
                .when()
                .post("/simulator")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("Started", response.jsonPath().getString("status"));  
    }
}
