package com.example.springbootr2dbcspannersampleapp;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static com.google.cloud.spanner.r2dbc.SpannerConnectionFactoryProvider.*;
import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandaloneSpannerEmulatorIT {

    private static final ConnectionFactory connectionFactory =
        ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(DRIVER, "spanner")
                .option(PROJECT, System.getProperty("gcp.project", "nv-local"))
                .option(INSTANCE, System.getProperty("spanner.instance", "test-instance"))
                .option(DATABASE, System.getProperty("spanner.database", "test-db"))
                .option(USE_PLAIN_TEXT, true)
                .build());

    @Autowired
    private ActivityDao activityDao;
    @Autowired
    private WebTestClient client;

    @BeforeAll
    static void initializeTestEnvironment() {
        Mono.from(connectionFactory.create())
            .flatMap(c -> Mono.from(c.createStatement("drop table activities").execute())
                .doOnSuccess(x -> log.info("Table drop completed."))
                .doOnError(
                    x -> {
                        if (!x.getMessage().contains("Table not found")) {
                            log.info("Table drop failed. {}", x.getMessage());
                        }
                    }
                )
                .onErrorResume(x -> Mono.empty())
                .thenReturn(c)
            )
            .flatMap(c -> Mono.from(c.createStatement(
                        "create table activities ("
                            + "  id int64 not null,"
                            + "  description string(1000) not null,"
                            + "  created_at timestamp not null"
                            + ") primary key (id)")
                    .execute())
                .doOnSuccess(x -> log.info("Table creation completed."))
            ).block();
    }

    @AfterAll
    static void cleanupTableAfterTest() {
        Mono.from(connectionFactory.create())
            .flatMap(c -> Mono.from(c.createStatement("drop table activities").execute())
                .doOnSuccess(x -> log.info("Table drop completed."))
                .doOnError(x -> log.info("Table drop failed."))
            ).block();
    }

    @Test
    void shouldInsertToAndSelectFromDbSuccessfully() {
        Activity a = new Activity(1L, "test activity", OffsetDateTime.parse("2021-12-15T21:30:10Z"));

        activityDao
            .insertOne(a)
            .as(StepVerifier::create)
            .expectNext(a)
            .verifyComplete();

        activityDao
            .selectOne(1L)
            .as(StepVerifier::create)
            .expectNextMatches(
                c ->
                    c.getId() == 1L &&
                        c.getDescription().equals("test activity") &&
                        c.getCreatedAt().isEqual(OffsetDateTime.parse("2021-12-15T21:30:10Z")))
            .verifyComplete();
    }

    @Test
    void shouldCreateAndFetchActivitySuccessfully() {
        Activity a = new Activity(2L, "test activity 2", null);

        client.post()
            .uri("/api/activity")
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(a), Activity.class)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("id").isEqualTo("2")
            .jsonPath("description").isEqualTo("test activity 2")
            .jsonPath("created_at").exists();

        client.get()
            .uri("/api/activity/2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("id").isEqualTo("2")
            .jsonPath("description").isEqualTo("test activity 2")
            .jsonPath("created_at").exists();
    }
}
