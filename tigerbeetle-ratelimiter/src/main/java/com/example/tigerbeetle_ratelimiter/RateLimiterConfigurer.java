package com.example.tigerbeetle_ratelimiter;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.Client;
import com.tigerbeetle.TransferBatch;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.tigerbeetle.AccountFlags.DEBITS_MUST_NOT_EXCEED_CREDITS;

@Configuration
@RequiredArgsConstructor
class RateLimiterConfigurer implements WebMvcConfigurer {

    public static final int OPERATOR = 1;
    public static final int USER = 2;

    private final Client client;
    private final ObservationRegistry observationRegistry;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        AccountBatch accounts = new AccountBatch(2);
        accounts.add();
        accounts.setId(OPERATOR);
        accounts.setLedger(1);
        accounts.setCode(1);

        accounts.add();
        accounts.setId(USER);
        accounts.setLedger(1);
        accounts.setCode(1);
        accounts.setFlags(DEBITS_MUST_NOT_EXCEED_CREDITS);

        TransferBatch transfers = new TransferBatch(1);
        transfers.add();
        transfers.setId(1);
        transfers.setDebitAccountId(OPERATOR);
        transfers.setCreditAccountId(USER);
        transfers.setLedger(1);
        transfers.setCode(1);
        transfers.setAmount(10);

        client.createTransfers(transfers);

        registry.addInterceptor(rateLimitInterceptor());
    }

    @Bean
    @RequestScope
    HandlerInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(client, observationRegistry);
    }
}

