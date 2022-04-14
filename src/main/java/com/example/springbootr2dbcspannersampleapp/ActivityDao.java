package com.example.springbootr2dbcspannersampleapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * @author vasilevn
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityDao {

    private final R2dbcEntityTemplate template;

    Mono<Activity> selectOne(long id) {
        return template.select(Activity.class)
            .matching(query(where("id").is(id)))
            .one();
    }

    Mono<Activity> insertOne(Activity a) {
        return template.insert(a);
    }
}
