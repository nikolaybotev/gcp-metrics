package com.nikolaybotev.metrics;


import java.io.Serializable;

public interface CounterWithLabel extends Serializable {
    default void inc(String labelValue) {
        inc(labelValue, 1);
    }

    void inc(String labelValue, long n);
}
