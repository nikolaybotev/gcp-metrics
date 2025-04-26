package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class FastReadWriteLock implements Serializable {
    public static final AtomicInteger readYield = new AtomicInteger();
    public static final AtomicInteger writeYield = new AtomicInteger();
    public static final AtomicInteger[] concurrentReaders =
            Arrays.stream(new Object[200]).map(n -> new AtomicInteger()).toArray(AtomicInteger[]::new);

    private final AtomicInteger counter = new AtomicInteger();
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    public class ReadLock implements AutoCloseable, Serializable {
        private ReadLock() {}

        @Override
        public void close() {
            var held = counter.getAndAdd(-2);
            if (held % 2 == 1 || held < 0) {
                throw new IllegalStateException("held by read lock " + held);
            }
            concurrentReaders[held / 2].incrementAndGet();
        }
    }

    public class WriteLock implements AutoCloseable, Serializable {
        private WriteLock() {}

        @Override
        public void close() {
            var held = counter.getAndDecrement();
            if (held % 2 == 0) {
                throw new IllegalStateException("held by write lock " + held);
            }
        }
    }

    public ReadLock acquireReadLock() {
        while (true) {
            // Optimistically acquire read lock
            var previous = counter.getAndAdd(2);
            if (previous % 2 == 1) {
                // Write lock is held ... release our lock and yield.
                counter.getAndAdd(-2);
                readYield.incrementAndGet();
                Thread.yield();
            } else {
                break;
            }
        }
        return readLock;
    }

    public WriteLock acquireWriteLock() {
        while (!counter.compareAndSet(0, 1)) {
            // Write lock not possible... yield.
            writeYield.incrementAndGet();
            Thread.yield();
        }
        return writeLock;
    }
}
