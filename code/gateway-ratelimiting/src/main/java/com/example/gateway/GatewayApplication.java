package com.example.gateway;

import lombok.extern.log4j.Log4j2;
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

/**
 * Show security and the RedisRateLimiter
 * Show the custom /hello endpoint @RestController in customers
 * Show the deluge.sh script
 * Show the return values in the http responses (429)
 * Show the result of running `keys *` in `redis-cli` while requests are being thrown at the service 
 */
@Log4j2
@SpringBootApplication
public class GatewayApplication {

    @Bean
    RedisRateLimiter rrl() {
        return new RedisRateLimiter(5,  10);
    }

    @Bean
    KeyResolver pks() {
        return new PrincipalNameKeyResolver();
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec ->
                        routeSpec
                                .path("/hi")
                                .filters(fs -> fs
                                        .requestRateLimiter(rlc -> rlc
                                                .setRateLimiter(rrl())
                                                .setKeyResolver(pks())
                                        )
                                        .setPath("/hello")
                                )
                                .uri("http://localhost:9191/")
                )
                .build();
    }


    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
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