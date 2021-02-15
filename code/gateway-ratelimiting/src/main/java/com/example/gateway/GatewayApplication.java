package com.example.gateway;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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


@Log4j2
@SpringBootApplication
public class GatewayApplication {

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity http) {
        return http
                .authorizeExchange(ae -> ae.pathMatchers("/customers"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(User.builder().username("jlong").password("pw").roles("USER").build());
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec -> routeSpec
                        .path("/customers")
                        .filters(fs -> fs
                                .setPath("/customers")
                                .requestRateLimiter(rlc -> rlc
                                        .setRateLimiter(new RedisRateLimiter(5, 2))
                                        .setKeyResolver(new PrincipalNameKeyResolver())
                                )
                        )
                        .uri("http://localhost:8080/")
                )
                .build();
    }


    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}

@Configuration
class SecurityConfiguration {


}