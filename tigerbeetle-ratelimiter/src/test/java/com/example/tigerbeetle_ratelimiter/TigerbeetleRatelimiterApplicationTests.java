package com.example.tigerbeetle_ratelimiter;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TigerbeetleRatelimiterApplicationTests {

    private static final int RATE_LIMIT = 10;
    private static final String GREETING_ENDPOINT = "/greeting";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestObservationRegistry observationRegistry;

    @Test
    void contextLoads() {
    }

    @ParameterizedTest
    @ValueSource(ints = {1, RATE_LIMIT - 1})
    void shouldAllowRequestsWithinRateLimit(int requestCount) {
        for (int i = 0; i < requestCount; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(GREETING_ENDPOINT, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void shouldRejectRequestsBeyondRateLimit() {
        for (int i = 0; i < RATE_LIMIT; i++) {
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