package com.example.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Josh Long
 */
@SpringBootApplication
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}

@RestController
class OrderRestController {

    private final Map<Integer, Collection<Order>> db = new ConcurrentHashMap<>();

    OrderRestController() {
        for (var customerId = 1; customerId < 10; customerId++) {
            var listOfOrders = new ArrayList<Order>();
            for (var orderId = 1; orderId < (Math.random() * 100) / 2; orderId++) {
                listOfOrders.add(new Order(customerId, orderId));
            }
            this.db.put(customerId, listOfOrders);
        }
    }

    @GetMapping("/orders/{customerId}")
    Flux<Order> getOrdersFor(@PathVariable Integer customerId) {
        return Flux.fromIterable(this.db.get(customerId));
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Order {
    private Integer id;
    private Integer customerId;
}