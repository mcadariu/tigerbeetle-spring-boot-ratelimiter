package com.example.tigerbeetle_ratelimiter;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.Client;
import com.tigerbeetle.CreateTransferResultBatch;
import com.tigerbeetle.IdBatch;
import com.tigerbeetle.TransferBatch;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Random;

import static com.example.tigerbeetle_ratelimiter.RateLimiterConfigurer.OPERATOR_ID;
import static com.tigerbeetle.AccountFlags.DEBITS_MUST_NOT_EXCEED_CREDITS;
import static com.tigerbeetle.CreateTransferResult.ExceedsCredits;
import static com.tigerbeetle.TransferFlags.PENDING;
import static io.micrometer.observation.Observation.Event.of;
import static io.micrometer.observation.Observation.start;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class RateLimitInterceptor implements HandlerInterceptor {

    //acquire this from your authentication system
    public static final int USER = new Random().nextInt();

    private final Client client;
    private final ObservationRegistry observationRegistry;

    public RateLimitInterceptor(Client client, ObservationRegistry observationRegistry) {
        this.client = client;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {

        IdBatch idBatch = new IdBatch(1);
        idBatch.add(USER);

        var userAccount = client.lookupAccounts(idBatch);

        if (!userAccount.next()) {
            AccountBatch accountBatch =  new AccountBatch(1);
            accountBatch.add();
            accountBatch.setId(USER);
            accountBatch.setLedger(1);
            accountBatch.setCode(1);
            accountBatch.setFlags(DEBITS_MUST_NOT_EXCEED_CREDITS);

            client.createAccounts(accountBatch);

            makeTransfer(10, OPERATOR_ID, USER, 0, 0);
        }

        CreateTransferResultBatch transferErrors = makeTransfer(1, USER, OPERATOR_ID, 5, PENDING);

        if (transferErrors.next() && transferErrors.getResult().equals(ExceedsCredits)) {
            Observation observation = start("ratelimit", observationRegistry);
            observation.event(of("limited"));
            observation.stop();
            response.setStatus(TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }

    private CreateTransferResultBatch makeTransfer(long amount, long debitAcct, long creditAcct, int timeout, int flag) {
        TransferBatch transfer = new TransferBatch(1);
        transfer.add();
        transfer.setId(new Random().nextInt());
        transfer.setDebitAccountId(debitAcct);
        transfer.setCreditAccountId(creditAcct);
        transfer.setLedger(1);
        transfer.setCode(1);
        transfer.setAmount(amount);
        transfer.setFlags(flag);
        transfer.setTimeout(timeout);

        return client.createTransfers(transfer);
    }
}
