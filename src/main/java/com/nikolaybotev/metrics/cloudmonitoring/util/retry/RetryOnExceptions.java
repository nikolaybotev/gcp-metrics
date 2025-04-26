package com.nikolaybotev.metrics.cloudmonitoring.util.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public class RetryOnExceptions implements Serializable {
    @Serial
    private static final long serialVersionUID = -9003263451982207959L;

    private static final Logger logger = LoggerFactory.getLogger(RetryOnExceptions.class);

    private final Duration timeout;
    private final int maxAttempts;
    private final Duration retryDelay;

    public RetryOnExceptions(Duration timeout,
                             int maxAttempts,
                             Duration retryDelay) {
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    public long run(Runnable op, Set<Class<? extends Exception>> exceptions) {
        final var timeoutMillis = timeout.toMillis();
        final var retryDelayMillis = retryDelay.toMillis();

        var attempt = 1;
        var startTime = Instant.now().toEpochMilli();
        while (true) {
            try {
                op.run();
                return Instant.now().toEpochMilli() - startTime;
            } catch (Exception ex) {
                // Check timeout
                var elapsed = Instant.now().toEpochMilli() - startTime;
                if (elapsed >= timeoutMillis) {
                    throw new RetryException("Timeout after " + elapsed + " ms", ex);
                }
                // Check max attempts
                if (attempt >= maxAttempts) {
                    throw new RetryException("Failed after " + attempt + " attempts", ex);
                }
                // Check if exception is retriable
                if (!exceptions.contains(ex.getClass())) {
                    throw new RetryException("Unsuccessful metrics publish", ex);
                }
                logger.info("Retrying metrics publish - attempt {} after {}: {}", attempt, ex.getClass(), ex.getMessage());
            }
            // Wait between retries
            try {
                //noinspection BusyWait
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException ex) {
                throw new RetryException("Interrupted while waiting to retry.", ex);
            }
            attempt += 1;
        }
    }
}
