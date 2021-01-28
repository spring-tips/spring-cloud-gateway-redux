package com.example.gateway;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@SpringBootApplication
public class GatewayApplication {

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec -> routeSpec.path("/hello").filters(fs -> fs.setPath("/guides")).uri("http://spring.io"))
                .route(routeSpec -> routeSpec.path("/customers.ws").filters(fs -> fs.setPath("/ws/customers")).uri("lb://customers/"))
                .build();
    }

    @Bean
    RouteLocator customRouteLocator(

            RewritePathGatewayFilterFactory rewritePathGatewayFilterFactory) {

        var rewritePathGatewayFilter = rewritePathGatewayFilterFactory
                .apply(config -> config
                        .setRegexp("\\/o\\/(?<segment>.*)\\.json")
                        .setReplacement("/orders/${segment}")
                );

        var singleRoute = Route//
                .async() //
                .id("orders-json-to-orders") //
//                .uri("lb://orders") //
                .asyncPredicate(request -> {
                    var uri = request.getRequest().getURI();
                    var path = uri.getPath();
                    var match = path.contains("o/");
                    log.debug("result for " + uri + '/' + path + " " + match);
                    return Mono.just(match);
                })
                .filter(new OrderedGatewayFilter(rewritePathGatewayFilter, 0)) //
                .filter(new OrderedGatewayFilter((exchange, chain) -> {
                    log.info("ASCII: " + exchange.getRequest().getURI()) ;
                    return chain.filter(exchange);
                }, 1))
                .uri("lb://orders")
                .build();

        return () -> Flux.just(singleRoute);//
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
