package org.acme.orders.infra.repo;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.acme.orders.domain.Order;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Singleton;

@Singleton
public class OrderRepositoryMem implements OrderRepository {
    Logger logger = Logger.getLogger(OrderRepositoryMem.class.getName());
    private static ConcurrentHashMap<String,Order> repo = new ConcurrentHashMap<String,Order>();

    private static ObjectMapper mapper = new ObjectMapper();
    private static String pattern = "yyyy-MM-dd  HH:mm:ssZ";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

    public OrderRepositoryMem() {
        super();
        
        InputStream is = getClass().getClassLoader().getResourceAsStream("orders.json");
        if (is == null) 
            throw new IllegalAccessError("file not found for order json");
        try {
            List<Order> currentDefinitions = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Order.class));
            currentDefinitions.stream().forEach( (t) -> repo.put(t.orderID,t));
        } catch (IOException e) {
            e.printStackTrace();
        }
        repo.values().stream().forEach(v -> System.out.println(v.toString()));
    }

    public List<Order> getAllOrders(){
        List<Order> allItems = new ArrayList<Order>(repo.values());
        return allItems;
    }

    public void addOrder(Order entity) {
        logger.info("Save in repository " + entity.orderID);
        entity.creationDate = simpleDateFormat.format(new Date());
        entity.updateDate = entity.creationDate;
        repo.put(entity.orderID, entity);
    }

    public void updateOrder(Order entity) {
        entity.updateDate = simpleDateFormat.format(new Date());
        repo.put(entity.orderID, entity);
    }

    @Override
    public Order findById(String key) {
        Order o = repo.get(key);
       logger.info(o.toString());
        return o;
    }
}
