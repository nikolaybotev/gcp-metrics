package com.nikolaybotev.metrics;


import javax.annotation.Nullable;
import java.io.Serializable;

public interface Counter extends Serializable {
    default void inc() {
        inc(1, null);
    }

    default void inc(long n) {
        inc(n, null);
    }

    void inc(long n, @Nullable String labelValue);
}
