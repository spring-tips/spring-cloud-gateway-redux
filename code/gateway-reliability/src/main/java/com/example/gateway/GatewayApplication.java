package com.example.basics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder routeLocatorBuilder) {

        return routeLocatorBuilder
                .routes()
                .route( rs -> rs   .path("/expensive")
                        .filters(fs -> fs
                                .requestRateLimiter(rlc -> rlc
                                        .setRateLimiter(rrl())
                                        .setKeyResolver(pks())
                                )
                                .setPath("/hello")
                        )
                        .uri("lb://customers"))
                .route(rs -> rs
                        .path("/default")
                        .filters(fs -> fs
                                .setPath("/")
                                .filter((exchange, chain) -> {
                                    System.out.println("new chance!");
                                    return chain.filter(exchange);
                                })
                        )
                        .uri("https://spring.io/guides")
                )
                .route(rs -> rs
                        .path("/customers")
                        .filters(gfs -> gfs.circuitBreaker(config -> config
                                .setFallbackUri("forward:/default")
                        ))
                        .uri("lb://customers")
                )
                .route(rs -> rs
                        .path("/error/**")
                        .filters(fs -> fs.retry(5))
                        .uri("lb://customers")
                )
                .build();
    }

    @Bean
    RedisRateLimiter rrl() {
        return new RedisRateLimiter(5, 10);
    }

    @Bean
    KeyResolver pks() {
        return new PrincipalNameKeyResolver();
    }

}


@Configuration
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity http) {
        return http
                .authorizeExchange(ae ->
                        ae.pathMatchers("/hi").authenticated()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(User.withDefaultPasswordEncoder().username("jlong").password("pw").roles("USER").build());
    }
}
