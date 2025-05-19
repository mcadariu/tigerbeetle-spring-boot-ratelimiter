package com.example.tigerbeetle_ratelimiter;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.Client;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
class RateLimiterConfigurer implements WebMvcConfigurer {

    public static final int OPERATOR_ID = 1;

    private final Client client;
    private final ObservationRegistry observationRegistry;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AccountBatch accounts = new AccountBatch(1);
        accounts.add();
        accounts.setId(OPERATOR_ID);
        accounts.setLedger(1);
        accounts.setCode(1);

        client.createAccounts(accounts);

        registry.addInterceptor(rateLimitInterceptor());
    }

    @Bean
    @RequestScope
    HandlerInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(client, observationRegistry);
    }
}

