package com.example.springbootr2dbcspannersampleapp;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Testcontainers
@ExtendWith(value = {SpringExtension.class, SystemStubsExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedSpannerEmulatorIT {

    private static final String PROJECT_ID = "nv-local";
    private static final String INSTANCE_ID = "test-instance";
    private static final String DATABASE_ID = "test-db";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final SpannerEmulatorContainer spannerContainer =
        new SpannerEmulatorContainer(
            DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator").withTag("1.4.1"));

    @Autowired
    private ActivityDao activityDao;
    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        // Spanner
        environmentVariables.set("SPANNER_EMULATOR_HOST", spannerContainer.getEmulatorGrpcEndpoint());
        r.add("spring.r2dbc.url", () -> "r2dbc:cloudspanner://" +
            "/projects/" + PROJECT_ID + "/instances/" + INSTANCE_ID + "/databases/" + DATABASE_ID);
    }

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        init();
    }

    private static void createInstance(Spanner spanner) throws InterruptedException, ExecutionException {
        InstanceConfigId instanceConfig = InstanceConfigId.of(PROJECT_ID, "emulator-config");
        InstanceId instanceId = InstanceId.of(PROJECT_ID, INSTANCE_ID);
        InstanceAdminClient insAdminClient = spanner.getInstanceAdminClient();
        insAdminClient.createInstance(
            InstanceInfo.newBuilder(instanceId).setNodeCount(1).setDisplayName("Test instance").setInstanceConfigId(instanceConfig).build()).get();
    }

    private static void createDatabase(Spanner spanner) throws InterruptedException, ExecutionException {
        DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();
        dbAdminClient.createDatabase(INSTANCE_ID, DATABASE_ID, List.of()).get();
    }

    @SneakyThrows
    private static void init() {
        SpannerOptions options = SpannerOptions.newBuilder()
            .setEmulatorHost(spannerContainer.getEmulatorGrpcEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .setProjectId(PROJECT_ID)
            .build();

        Spanner s = options.getService();
        createInstance(s);
        createDatabase(s);

        DatabaseAdminClient dbaClient = s.getDatabaseAdminClient();
        dbaClient.updateDatabaseDdl(INSTANCE_ID, DATABASE_ID, List.of(
            "create table activities ("
            + "  id int64 not null,"
            + "  description string(1000) not null,"
            + "  created_at timestamp not null"
            + ") primary key (id)"), null)
            .get();
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
