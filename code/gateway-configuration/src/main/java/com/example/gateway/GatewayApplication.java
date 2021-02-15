package com.example.gateway;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Here are some of the URLs that work:
 *
 * <LI> http://localhost:9292/o/3.json </LI>
 * <li> Hit the /actuator/refresh endpoint with <code>curl -XPOST http://localhost:8010/actuator/refresh </code> </li>
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

    private final AtomicBoolean updateRoutes = new AtomicBoolean(false);

    @Bean
    @RefreshScope
    RouteLocator routeLocator(RouteLocatorBuilder rlb) {

        RouteLocatorBuilder.Builder routes = rlb
                .routes();
        var id = "customers";
        var customersServiceLocal = "http://localhost:9191";
        if (!this.updateRoutes.get()) {
            routes.route(id, rs -> rs.path("/c").filters(f -> f.setPath("/hello")).uri(customersServiceLocal));
        } //
        else {
            routes.route(id, rs -> rs.path("/c").filters(f -> f.setPath("/customers")).uri(customersServiceLocal));
            routes.route(id, rs -> rs.path("/hello").filters(f -> f.setPath("/hello")).uri(customersServiceLocal));
        }
        this.updateRoutes.set(true);
        return routes.build();
    }


    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
