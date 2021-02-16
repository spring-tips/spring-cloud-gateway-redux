package com.example.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * This demonstrates how to run an HTTP APi that's protected as an OAuth resource.
 * <p>
 * You'll need to run `mvn spring-boot:run` in the root of the `authorization-service` module.
 * <p>
 * Urls that work:
 * - we have a custom servlet context. so the URL is http://localhost:9090/resource-server-jwt/greetings
 */
@SpringBootApplication
public class ResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceApplication.class, args);
    }

}


@Configuration
class JWTSecurityConfiguration
        extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {// @formatter:off
        http
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/hello").hasAuthority("SCOPE_read")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
    }
}


@RestController
class GreetingsRestController {

    @GetMapping("/greetings")
    Map<String, String> hello(/*@AuthenticationPrincipal Authentication jwt*/) {
        return Collections.singletonMap("message", "Hello, world");
    }

}