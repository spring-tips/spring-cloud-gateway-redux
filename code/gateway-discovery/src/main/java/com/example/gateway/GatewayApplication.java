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
 * <LI> http://localhost:9292/customers/customers to show the auto route creation </LI>
 * <LI> http://localhost:9292/new-customers.ws to show manually doing client side loadbalancing </LI>

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
                .route(routeSpec -> routeSpec.path("/new-customers.ws").filters(fs -> fs.setPath("/ws/customers")).uri("lb://customers/")) // websockets
                .build();
    }


    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
