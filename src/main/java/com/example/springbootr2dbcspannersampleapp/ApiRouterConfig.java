package com.example.springbootr2dbcspannersampleapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ApiRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> routerConfig(ActivityRequestHandler activityRequestHandler) {
        return route()
            .path("/api", b1 -> b1
                .nest(accept(MediaType.APPLICATION_JSON), b2 -> b2
                    .POST("/activity", activityRequestHandler::createOne)
                    .GET("/activity/{id}", activityRequestHandler::getOne))
            ).build();
    }
}