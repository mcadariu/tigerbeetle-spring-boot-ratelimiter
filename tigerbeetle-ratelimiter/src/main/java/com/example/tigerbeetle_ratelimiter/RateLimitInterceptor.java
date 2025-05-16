package com.example.tigerbeetle_ratelimiter;

import com.tigerbeetle.Client;
import com.tigerbeetle.CreateTransferResultBatch;
import com.tigerbeetle.TransferBatch;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Random;

import static com.tigerbeetle.CreateTransferResult.ExceedsCredits;
import static com.tigerbeetle.TransferFlags.PENDING;
import static io.micrometer.observation.Observation.Event.of;
import static io.micrometer.observation.Observation.start;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class RateLimitInterceptor implements HandlerInterceptor {

    public static final int OPERATOR = 1;
    public static final int USER = 2;

    private final Client client;
    private final ObservationRegistry observationRegistry;

    public RateLimitInterceptor(Client client, ObservationRegistry observationRegistry) {
        this.client = client;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        TransferBatch transfers = new TransferBatch(1);
        transfers.add();
        transfers.setId(new Random().nextInt());
        transfers.setDebitAccountId(USER);
        transfers.setCreditAccountId(OPERATOR);
        transfers.setLedger(1);
        transfers.setCode(1);
        transfers.setAmount(1);
        transfers.setFlags(PENDING);
        transfers.setTimeout(5);

        CreateTransferResultBatch transferErrors = client.createTransfers(transfers);

        if (transferErrors.next() && transferErrors.getResult().equals(ExceedsCredits)) {
            Observation observation = start("ratelimit", observationRegistry);
            observation.event(of("limited"));
            observation.stop();
            response.setStatus(TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }
}
