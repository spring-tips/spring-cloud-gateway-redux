package com.example.gateway;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;


/**
 * This demonstrates an API gateway that acts as a token relay for downstream resource servers.
 * <p>
 * Here are some of the URLs that work:
 */
@Log4j2
@SpringBootApplication
public class GatewayApplication {

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec -> routeSpec.path("/hi").filters(fs -> fs.setPath("/greetings")).uri("http://localhost:8087/greetings")) // websockets
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
