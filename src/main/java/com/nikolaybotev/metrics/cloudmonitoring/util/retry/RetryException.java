package com.nikolaybotev.metrics.cloudmonitoring.util.retry;

public class RetryException extends RuntimeException {
    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
