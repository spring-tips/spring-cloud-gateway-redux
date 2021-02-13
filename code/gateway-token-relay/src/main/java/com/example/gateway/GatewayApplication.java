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
 * Here are some of the URLs that work:
 *
 * <LI> http://localhost:9292/customers/customers </LI>
 * <LI> http://localhost:9292/orders/orders/2 </LI>
 * <LI> http://localhost:9292/hello </LI>
 * <LI> http://localhost:9292/new-customers.ws </LI>
 * <LI> http://localhost:9292/index.html </LI>
 * <LI> http://localhost:9292/actuator/gateway </LI>
 * <LI> http://localhost:9292/actuator/metrics/spring.cloud.gateway.requests </LI>
 * <LI> http://localhost:9292/o/3.json </LI>
 * <LI> http://localhost:9292/twitter/@starbuxman </LI>
 */
@Log4j2
@SpringBootApplication
public class GatewayApplication {


    @Bean
    ApplicationListener<RefreshRoutesResultEvent> routesRefreshed() {
        return rre -> {
            log.info(rre.getClass().getSimpleName());
            Assert.state(rre.getSource() instanceof CachingRouteLocator, () -> "the " + rre.getClass().getName() + " routes must be refreshed");
            CachingRouteLocator source = (CachingRouteLocator) rre.getSource();
            Flux<Route> routes = source.getRoutes();
            routes.subscribe(route -> log.info(route.getClass() + ":"
                    + route.getMetadata().toString() + ":" + route.getFilters()));
        };
    }


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

    @Bean
    RouteLocator customRouteLocator(RewritePathGatewayFilterFactory rewritePathGatewayFilterFactory) {

        var rewritePathGatewayFilter = rewritePathGatewayFilterFactory
                .apply(config -> config
                        .setRegexp("\\/o\\/(?<segment>.*)\\.json")
                        .setReplacement("/orders/${segment}")
                );

        var singleRoute = Route//
                .async() //
                .id("orders-json-to-orders") //
                .asyncPredicate(request -> {
                    var uri = request.getRequest().getURI();
                    var path = uri.getPath();
                    var match = path.contains("o/");
                    log.debug("result for " + uri + '/' + path + " " + match);
                    return Mono.just(match);
                })
                .filter(new OrderedGatewayFilter(rewritePathGatewayFilter, 0)) //
                .filter(new OrderedGatewayFilter((exchange, chain) -> {
                    log.info("new URI: " + exchange.getRequest().getURI());
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
