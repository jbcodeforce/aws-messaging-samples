package org.acme.jms;

import org.acme.jms.model.CarRide;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestCarRide {
    @Test
    public void test() throws JsonProcessingException {
        CarRide cr = CarRide.createRandomQRide();
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(cr));
    }
}
