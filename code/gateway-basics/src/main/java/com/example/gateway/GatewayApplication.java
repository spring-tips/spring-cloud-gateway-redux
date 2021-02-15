package com.example.gateway;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * show hwo to create a simple RouteLocator
 * show the basic structure of one route with a filter or two
 * show http, ws
 * show actuator
 * Here are some of the URLs that work:
 *
 *
 * <LI> http://localhost:9292/customers/customers </LI>
 * <LI> http://localhost:9292/orders/orders/2 </LI>
 * <LI> http://localhost:9292/hello </LI>
 * <LI> http://localhost:9292/new-customers.ws </LI>
 * <LI> http://localhost:9292/index.html </LI>
 * <LI> http://localhost:9292/actuator/gateway </LI>
 * <LI> http://localhost:9292/actuator/metrics/spring.cloud.gateway.requests </LI>
 * <LI> http://localhost:9292/twitter/@starbuxman </LI>
 */
@Log4j2
@SpringBootApplication
public class GatewayApplication {

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec -> routeSpec.path("/hello").filters(fs -> fs.setPath("/guides")).uri("http://spring.io")) // http
                .route(routeSpec -> routeSpec.path("/new-customers.ws").filters(fs -> fs.setPath("/ws/customers")).uri("lb://customers/")) // websockets
                .route(rs -> rs
                        .path("/twitter/@**")
                        .filters(c -> c.rewritePath("/twitter/@(?<handle>.*)", "/${handle}"))
                        .uri("http://twitter.com/")
                )
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
