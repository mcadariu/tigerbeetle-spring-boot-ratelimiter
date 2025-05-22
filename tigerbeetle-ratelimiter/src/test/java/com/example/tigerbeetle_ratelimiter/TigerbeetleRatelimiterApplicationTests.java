package com.example.tigerbeetle_ratelimiter;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.File;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TigerbeetleRatelimiterApplicationTests {

    private static final String GREETING_ENDPOINT = "/greeting";

    @Container
    public static DockerComposeContainer<?> environment =
            new DockerComposeContainer<>(new File("docker-compose.yml"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestObservationRegistry observationRegistry;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRejectRequestsBeyondRateLimit() {
        for (int i = 0; i < 2; i++) {
            restTemplate.getForEntity(GREETING_ENDPOINT, String.class);
        }

        // The next request should be rate limited
        ResponseEntity<String> response = restTemplate.getForEntity(GREETING_ENDPOINT, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("ratelimit")
                .that()
                .hasBeenStarted()
                .hasBeenStopped();
    }

    @TestConfiguration
    static class ObservationTestConfiguration {

        @Bean
        TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
    }
}