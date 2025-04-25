package com.nikolaybotev.metrics;

import java.io.Serializable;

public interface Distribution extends Serializable {
    void update(long value);
}
