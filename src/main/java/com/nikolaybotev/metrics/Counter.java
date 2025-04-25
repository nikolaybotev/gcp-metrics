package com.nikolaybotev.metrics;


import java.io.Serializable;

public interface Counter extends Serializable {
    default void inc() {
        inc(1);
    }

    void inc(long n);
}
