package com.example.customers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@SpringBootApplication
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

    private final String[] names =
            "Jean,Yuxin,Mario,Zhen,Mia,Maria,Dave,Johan,Francoise,Jose,Ibrahim".split(",");


    private final AtomicInteger counter = new AtomicInteger();

    private final Flux<Customer> customers = Flux.fromStream(
            Stream.generate(() -> {
                var id = counter.incrementAndGet();
                return new Customer(id, names[id % names.length]);
            }))
            .delayElements(Duration.ofSeconds(3));

    @Bean
    Flux<Customer> customers() {
        return this.customers.publish().autoConnect();
    }


}


@Configuration
@RequiredArgsConstructor
class CustomerWebSocketConfiguration {


    private final ObjectMapper objectMapper;

    @SneakyThrows
    private String from(Customer customer) {
        return this.objectMapper.writeValueAsString(customer);
    }

    @Bean
    WebSocketHandler webSocketHandler(Flux<Customer> customerFlux) {
        return webSocketSession -> {

            var map = customerFlux
                    .map(customer -> from(customer))
                    .map(webSocketSession::textMessage);

            return webSocketSession.send(map);
        };
    }

    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler customersWsh) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/customers", customersWsh), 10);
    }
}

@RestController
@RequiredArgsConstructor
class CustomerRestController {

    private final Flux<Customer> customerFlux;

    @GetMapping(
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            value = "/customers"
    )
    Flux<Customer> get() {
        return this.customerFlux;
    }

}

@RestController
class ReliabilityRestController {

    private final Map<String, AtomicInteger> errorCount = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> secondToCount = new ConcurrentHashMap<>();

    @GetMapping("/hello")
    String hello() {

        var now = System.currentTimeMillis();
        var second = (now / 1000);
        var countForTheSecond = this.secondToCount.compute(second, (aLong, value) -> {
            if (value == null) value = new AtomicInteger(0);
            value.incrementAndGet();
            return value;
        });
        System.out.println("there have been " + countForTheSecond + " attempts for the second " + second + '.');
        return "Hello, world";
    }


    @GetMapping("/error/{id}")
    ResponseEntity<?> errors(@PathVariable String id) {

        this.errorCount.compute(id,
                (s, value) -> {
                    if (value == null) {
                        value = new AtomicInteger(0);
                    }
                    value.incrementAndGet();
                    return value;
                });


        var count = this.errorCount.get(id).get();
        if (count < 5) {
            System.out.println("returning an error for request #" + count + " for ID '" + id + "'");
            return ResponseEntity.status(SERVICE_UNAVAILABLE).build();
        } else {
            System.out.println("returning a proper response for request #" + count + " for ID '" + id + "'");
        }
        return ResponseEntity.ok(
                Map.of("greeting", "Congrats, " + id + "! You did it!"));
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    private Integer id;
    private String name;
}
