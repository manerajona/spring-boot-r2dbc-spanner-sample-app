package com.example.springbootr2dbcspannersampleapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@RequiredArgsConstructor
@Component
public class ActivityRequestHandler {

    private final ActivityDao dao;

    public Mono<ServerResponse> getOne(ServerRequest request) {
        int id = Integer.parseInt(request.pathVariable("id"));
        return dao.selectOne(id)
            .flatMap(person -> ok().contentType(APPLICATION_JSON).bodyValue(person))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createOne(ServerRequest request) {
        return request.bodyToMono(Activity.class)
            .flatMap(a -> dao.insertOne(Activity.of(a, OffsetDateTime.now())))
            .flatMap(a -> ok().bodyValue(a));
    }
}