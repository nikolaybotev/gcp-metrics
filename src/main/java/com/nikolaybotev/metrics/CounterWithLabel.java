package com.nikolaybotev.metrics;


import java.io.Serializable;

public interface CounterWithLabel extends Serializable {
    default void inc(String ... labelValue) {
        inc(1, labelValue);
    }

    default void inc(String labelValue, long n) {
        inc(n, labelValue);
    }

    void inc(long n, String ... labelValue);
}
