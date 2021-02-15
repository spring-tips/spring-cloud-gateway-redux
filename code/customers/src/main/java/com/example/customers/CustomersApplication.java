package com.example.customers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@SpringBootApplication
@RequiredArgsConstructor
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

}

@Configuration
@RequiredArgsConstructor
class WebSocketConfiguration {

    private final ObjectMapper objectMapper;

    private final AtomicInteger counter = new AtomicInteger();

    private final String[] names =
            ("Jean,Yuxin,Mario,Zhen,Mia,Maria,Dave,Johan,Francoise,Jose,Ibrahim")
                    .split(",");

    private final Flux<Customer> customers = Flux
            .fromStream(Stream.generate(() -> {
                var id = counter.incrementAndGet();
                return new Customer(id, this.names[id % this.names.length]);
            }))
            .delayElements(Duration.ofSeconds(3));


    @Bean
    Flux<Customer> customers() {
        return this.customers.publish().autoConnect();
    }

    @Bean
    SimpleUrlHandlerMapping handlerMapping(
            WebSocketHandler wsh) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/customers", wsh), 10);
    }

    @Bean
    WebSocketHandler webSocketHandler() {
        return session -> session
                .send(this.customers()
                        .map(this::toJson)
                        .map(session::textMessage)
                );
    }

    @SneakyThrows
    private String toJson(Object customer) {
        return objectMapper.writeValueAsString(customer);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}

@RestController
class CustomerRestController {

    private final Flux<Customer> customers;

    CustomerRestController(Flux<Customer> customers) {
        this.customers = customers;
    }


    @GetMapping(
            value = "/customers",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    Flux<Customer> get() {
        return this.customers;
    }
}

// dont create this until the rate limiting section
@RestController
class RateLimitingRestController {

    private final Map<Integer, AtomicInteger> secondToCount = new ConcurrentHashMap<>();
    private final Calendar calendar = Calendar.getInstance();
    private final Object monitor = new Object();

    @GetMapping("/hello")
    String hello() {
        synchronized (this.calendar) {
            var second = calendar.get(Calendar.SECOND);
            secondToCount.putIfAbsent(second, new AtomicInteger(0));
            var countForTheSecond = secondToCount.get(second).incrementAndGet();
            System.out.println("there have been " + countForTheSecond + " attempts for the second " + second + '.');
        }

        // todo capture the current second and how many requests we've seen in this second
        return "Hello, world";
    }

}