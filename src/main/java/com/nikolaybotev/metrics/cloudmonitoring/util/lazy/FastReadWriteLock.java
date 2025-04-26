package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class FastReadWriteLock implements Serializable {
    private final AtomicInteger counter = new AtomicInteger();
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    public class ReadLock implements AutoCloseable, Serializable {
        private ReadLock() {}

        @Override
        public void close() {
            var prev = counter.getAndAdd(-2);
            assert prev % 2 == 0 && prev >= 0;
        }
    }

    public class WriteLock implements AutoCloseable, Serializable {
        private WriteLock() {}

        @Override
        public void close() {
            var prev = counter.getAndDecrement();
            assert prev % 2 == 0;
        }
    }

    public ReadLock acquireReadLock() {
        // Optimistically acquire a read lock.
        var previous = counter.getAndAdd(2);
        while (previous % 2 == 1) {
            // Write lock is held... release our lock and yield.
            counter.getAndAdd(-2);
            Thread.yield();

            // Try again.
            previous = counter.getAndAdd(2);
        }
        return readLock;
    }

    public WriteLock acquireWriteLock() {
        while (!counter.compareAndSet(0, 1)) {
            // Read or Write lock is held... yield.
            Thread.yield();
        }
        return writeLock;
    }
}
