package com.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CacheBenchmark {

    private static final int NUM_ENTRIES = 10000000;
    private static final int NUM_THREADS = 10;

    public static void main(String[] args) throws ExecutionException {
        // Benchmark Guava Cache
        double[] guavaResult = benchmarkGuavaCache();

        // Benchmark Caffeine Cache
        double[] caffeineResult = benchmarkCaffeineCache();

        // Benchmark ConcurrentHashMap
        double[] concurrentHashMapResult = benchmarkConcurrentHashMap();

        compareCachePerformance(guavaResult[1], caffeineResult[1], concurrentHashMapResult[1]);

    }

    private static double[] benchmarkGuavaCache() throws ExecutionException {
        com.google.common.cache.Cache<String, String> cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_ENTRIES)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();

        return runBenchmark(cache, "Guava Cache");
    }

    private static double[] benchmarkCaffeineCache() throws ExecutionException {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(NUM_ENTRIES)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();

        return runBenchmark(cache, "Caffeine Cache");
    }

    private static double[] benchmarkConcurrentHashMap() {
        ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

        return runBenchmark(cache, "ConcurrentHashMap");
    }

    private static double[] runBenchmark(Object cache, String cacheName) {
        long startTime = System.nanoTime();

        Runnable task = () -> {
            for (int i = 0; i < NUM_ENTRIES; i++) {
                String key = "Key" + i;
                try {
                    if (cache instanceof com.google.common.cache.Cache) {
                        ((com.google.common.cache.Cache<String, String>) cache).get(key, () -> fetchDataFromDataSource(key));
                    } else if (cache instanceof Cache) {
                        ((Cache<String, String>) cache).get(key, k -> fetchDataFromDataSource(k));
                    } else if (cache instanceof ConcurrentHashMap) {
                        ((ConcurrentHashMap<String, String>) cache).computeIfAbsent(key, k -> fetchDataFromDataSource(k));
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        double seconds = (double) elapsedTime / 1_000_000_000.0;
        double throughput = (NUM_ENTRIES * NUM_THREADS) / seconds;

        double[] result = new double[2];
        result[0] = seconds;
        result[1] = throughput;

        return result;
    }

    private static String fetchDataFromDataSource(String key) {
        return key.toUpperCase();
    }

    private static void compareCachePerformance(double guavaThroughput, double caffeineThroughput, double concurrentHashMapThroughput) {
        System.out.println("Cache performance comparison:");
        System.out.println("Guava Cache throughput: " + guavaThroughput + " operations per second");
        System.out.println("Caffeine Cache throughput: " + caffeineThroughput + " operations per second");
        System.out.println("ConcurrentHashMap throughput: " + concurrentHashMapThroughput + " operations per second");

        double guavaToCaffeineRatio = guavaThroughput / caffeineThroughput;
        double guavaToConcurrentHashMapRatio = guavaThroughput / concurrentHashMapThroughput;
        double caffeineToConcurrentHashMapRatio = caffeineThroughput / concurrentHashMapThroughput;

        System.out.println("Guava Cache to Caffeine Cache ratio: " + guavaToCaffeineRatio);
        System.out.println("Guava Cache to ConcurrentHashMap ratio: " + guavaToConcurrentHashMapRatio);
        System.out.println("Caffeine Cache to ConcurrentHashMap ratio: " + caffeineToConcurrentHashMapRatio);
    }
}
