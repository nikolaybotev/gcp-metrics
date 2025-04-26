package com.nikolaybotev.metrics.cloudmonitoring.util;

import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazyFast;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazySync;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazySync2;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SerializableLazyTest {
    @Test
    public void readWriteLock_performanceTest() {
        var randomClearThreshold = 0.99999999;
        var loadingCounter = new AtomicInteger();
        var lazy = new SerializableLazySync2<>(() -> {
            System.out.printf("Loading %d...", loadingCounter.incrementAndGet());
            var result = computePi(10_000);
            System.out.println(" done.");
            return result;
        });
        var startTime = System.currentTimeMillis();
        var threads = Runtime.getRuntime().availableProcessors() * 10;
        var tasks = threads * 10;
        System.out.printf("Running %d tasks on %d threads.%n", tasks, threads);
        var checksum = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(threads)) {
            for (var i = 0; i < tasks; i++) {
                executor.execute(() -> {
                    var rand = new Random();
                    var counter = 0;
                    for (var j = 0; j < 400_000; j++) {
                        counter += System.identityHashCode(lazy.getValue().abs());
                        if (rand.nextDouble() > randomClearThreshold) {
                            lazy.clear();
                        }
                    }
                    checksum.addAndGet(counter);
                });
            }
        }
        var elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Elapsed %.3f s (checksum %d)%n", elapsed / 1_000d, checksum.get());
        System.out.println("Stats: ");
//        System.out.println("     readYield: " + FastReadWriteLock.readYield.get());
//        System.out.println("    writeYield: " + FastReadWriteLock.writeYield.get());
//        for (var n = 0; n < FastReadWriteLock.concurrentReaders.length; n++) {
//            var val = FastReadWriteLock.concurrentReaders[n].get();
//            if (val != 0) {
//                System.out.printf("  %2d concurrent: %d%n", n, val);
//            }
//        }
    }

    public static BigDecimal computePi(int digits) {
        // Set the precision to the required number of digits plus some extra for intermediate calculations
        MathContext mc = new MathContext(digits + 5, RoundingMode.HALF_UP);

        BigDecimal a = BigDecimal.ONE; // a0 = 1
        BigDecimal b = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(Math.sqrt(2)), mc); // b0 = 1/sqrt(2)
        BigDecimal t = BigDecimal.valueOf(0.25); // t0 = 1/4
        BigDecimal p = BigDecimal.ONE; // p0 = 1

        BigDecimal aNext, bNext, tNext, pNext;

        for (int i = 0; i < 10; i++) { // 10 iterations should be sufficient for high precision
            aNext = a.add(b).divide(BigDecimal.valueOf(2), mc); // a(i+1) = (a(i) + b(i)) / 2
            bNext = BigDecimal.valueOf(Math.sqrt(a.doubleValue() * b.doubleValue())); // b(i+1) = sqrt(a(i) * b(i))
            tNext = t.subtract(p.multiply(a.subtract(aNext).pow(2, mc), mc), mc); // t(i+1) = t(i) - p(i) * (a(i) - a(i+1))^2
            pNext = p.multiply(BigDecimal.valueOf(2), mc); // p(i+1) = 2 * p(i)

            a = aNext;
            b = bNext;
            t = tNext;
            p = pNext;
        }

        // π = (a + b)^2 / (4 * t)
        BigDecimal pi = a.add(b).pow(2, mc).divide(p.multiply(BigDecimal.valueOf(4), mc), mc);

        return pi.round(new MathContext(digits, RoundingMode.HALF_UP)); // Round to the specified number of digits
    }
}
