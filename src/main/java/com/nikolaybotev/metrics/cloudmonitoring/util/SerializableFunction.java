package com.nikolaybotev.metrics.cloudmonitoring.util;

public interface SerializableFunction<I, O> {
    O apply(I input);
}
