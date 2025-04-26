package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class FasterReadWriteLock implements Serializable {
    @Serial
    private static final long serialVersionUID = -5485849394762641923L;

    private static final int bins = Runtime.getRuntime().availableProcessors();
    private static final ThreadLocal<Integer> bin = ThreadLocal.withInitial(() -> Thread.currentThread().hashCode() % bins);

    private static class ReadWriteSpinLock implements Serializable {
        private final AtomicInteger counter = new AtomicInteger();

        void acquireRead() {
            // Optimistically acquire a read lock.
            var previous = counter.getAndAdd(2);
            while (previous % 2 == 1) {
                // Write lock is held... release our lock and yield.
                counter.getAndAdd(-2);
                Thread.yield();

                // Try again.
                previous = counter.getAndAdd(2);
            }
        }

        void releaseRead() {
            var prev = counter.getAndAdd(-2);
            if (prev % 2 == 1 || prev < 0) {
                throw new IllegalStateException("read prev close " + prev);
            }
        }

        void acquireWrite() {
            while (!counter.compareAndSet(0, 1)) {
                Thread.yield();
            }
        }

        void releaseWrite() {
            var prev = counter.getAndDecrement();
            if (prev % 2 != 1) {
                throw new IllegalStateException("write prev close " + prev);
            }
        }
    }

    private final ReadWriteSpinLock writer = new ReadWriteSpinLock();
    private final ReadWriteSpinLock[] readers = new ReadWriteSpinLock[bins];
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    public class ReadLock implements AutoCloseable, Serializable {
        private ReadLock() {}

        @Override
        public void close() {
            readers[bin.get()].releaseRead();
        }
    }

    public class WriteLock implements AutoCloseable, Serializable {
        private WriteLock() {}

        @Override
        public void close() {
            for (var reader : readers) {
                reader.releaseWrite();
            }
            writer.releaseWrite();
        }
    }

    public FasterReadWriteLock() {
        for (var i = 0; i < readers.length; i++) {
            readers[i] = new ReadWriteSpinLock();
        }
    }

    public ReadLock acquireReadLock() {
        // Optimistically acquire a read lock.
        readers[bin.get()].acquireRead();
        return readLock;
    }

    public WriteLock acquireWriteLock() {
        // Acquire write-specific lock first.
        writer.acquireWrite();
        for (var reader : readers) {
            reader.acquireWrite();
        }
        return writeLock;
    }
}
