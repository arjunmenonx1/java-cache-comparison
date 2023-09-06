package com.example;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadingCacheExample {

    private final LoadingCache<String, String> loadingCache;
    private final ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
    private final AtomicInteger cacheMissesWithLoadingCache = new AtomicInteger();
    private final AtomicInteger cacheMissesWithConcurrentMap = new AtomicInteger();

    public LoadingCacheExample() {
        loadingCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .recordStats()
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) {
                        cacheMissesWithLoadingCache.incrementAndGet();
                        return fetchDataFromDataSource(key);
                    }
                });

        // Put an initial entry into the cache
        loadingCache.put("initial", "INITIAL-CACHED-VALUE");
    }

    private String fetchDataFromDataSource(String key) {
        return key.toUpperCase();
    }

    public String getUsingLoadingCache(String key) {
        try {
            return loadingCache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error fetching from cache", e);
        }
    }

    public String getUsingConcurrentMap(String key) {
        return concurrentMap.computeIfAbsent(key, k -> {
            cacheMissesWithConcurrentMap.incrementAndGet();
            return fetchDataFromDataSource(k);
        });
    }

    public static void main(String[] args) throws InterruptedException {
        LoadingCacheExample example = new LoadingCacheExample();

        // Fetching the initially populated value
        System.out.println("Initial cache value: " + example.getUsingLoadingCache("initial"));

        // Using LoadingCache
        System.out.println(example.getUsingLoadingCache("hello"));
        System.out.println(example.getUsingLoadingCache("hello"));

        // Using ConcurrentHashMap
        System.out.println(example.getUsingConcurrentMap("world"));
        System.out.println(example.getUsingConcurrentMap("world"));

        System.out.println("Cache misses with LoadingCache: " + example.cacheMissesWithLoadingCache.get());
        System.out.println("Cache misses with ConcurrentHashMap: " + example.cacheMissesWithConcurrentMap.get());

        // Wait for 6 seconds to allow cache entries to expire in LoadingCache
        Thread.sleep(6000);

        // Fetching again after entries are expired in LoadingCache
        System.out.println("After expiration:");
        System.out.println(example.getUsingLoadingCache("hello"));
        System.out.println("Cache misses with LoadingCache: " + example.cacheMissesWithLoadingCache.get());
    }
}
