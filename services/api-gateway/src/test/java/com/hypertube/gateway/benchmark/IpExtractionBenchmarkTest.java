package com.hypertube.gateway.benchmark;

import org.junit.jupiter.api.Test;

public class IpExtractionBenchmarkTest {

    @Test
    public void runBenchmark() {
        System.out.println("Starting Benchmark...");
        String singleIp = "192.168.1.1";
        String multiIp = "192.168.1.1, 10.0.0.1, 172.16.0.1";

        // Warmup
        for (int i = 0; i < 100_000; i++) {
            original(singleIp);
            original(multiIp);
            optimized(singleIp);
            optimized(multiIp);
        }

        // Benchmark
        long iterations = Boolean.getBoolean("fullBenchmark") ? 10_000_000 : 1_000;

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            original(singleIp);
        }
        long durationOriginalSingle = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            optimized(singleIp);
        }
        long durationOptimizedSingle = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            original(multiIp);
        }
        long durationOriginalMulti = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            optimized(multiIp);
        }
        long durationOptimizedMulti = System.nanoTime() - start;

        System.out.println("Results (" + iterations + " iterations):");
        System.out.printf("Original Single IP: %d ms%n", durationOriginalSingle / 1_000_000);
        System.out.printf("Optimized Single IP: %d ms%n", durationOptimizedSingle / 1_000_000);
        System.out.printf("Original Multi IP: %d ms%n", durationOriginalMulti / 1_000_000);
        System.out.printf("Optimized Multi IP: %d ms%n", durationOptimizedMulti / 1_000_000);
    }

    private String original(String forwardedFor) {
        return forwardedFor.split(",")[0].trim();
    }

    private String optimized(String forwardedFor) {
        int index = forwardedFor.indexOf(',');
        if (index == -1) {
            return forwardedFor.trim();
        }
        return forwardedFor.substring(0, index).trim();
    }
}
