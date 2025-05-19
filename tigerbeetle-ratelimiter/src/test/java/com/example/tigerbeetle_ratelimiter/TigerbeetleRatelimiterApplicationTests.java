package com.example.tigerbeetle_ratelimiter;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.fail;
import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TigerbeetleRatelimiterApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestObservationRegistry observationRegistry;

    @Test
    void contextLoads() {
    }

    @RepeatedTest(11)
    void rateLimiting(RepetitionInfo repetitionInfo) {
        HttpStatusCode statusCode = this.restTemplate.getForEntity("http://localhost:" + port + "/greeting",
                String.class).getStatusCode();

        if (statusCode == HttpStatus.OK) {
            return;
        } else if (statusCode == TOO_MANY_REQUESTS
                && repetitionInfo.getCurrentRepetition() == 11) {
            assertThat(observationRegistry)
                    .hasObservationWithNameEqualTo("ratelimit")
                    .that()
                    .hasBeenStarted()
                    .hasBeenStopped();
            return;
        } else {
            fail("Unsuccessful test");
        }
    }

    @TestConfiguration
    static class ObservationTestConfiguration {

        @Bean
        TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
    }
}
