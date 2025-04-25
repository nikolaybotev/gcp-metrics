package com.nikolaybotev.metrics.cloudmonitoring.util;

public class RetryException extends RuntimeException {
    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
