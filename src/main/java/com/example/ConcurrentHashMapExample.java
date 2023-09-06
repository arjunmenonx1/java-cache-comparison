package com.example;

        import java.util.concurrent.ConcurrentHashMap;
        import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHashMapExample {

    private final ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
    private final AtomicInteger cacheMissesWithConcurrentMap = new AtomicInteger();

    public ConcurrentHashMapExample() {
        // Put an initial entry into the cache
        concurrentMap.put("initial", "INITIAL-CACHED-VALUE");
    }

    private String fetchDataFromDataSource(String key) {
        return key.toUpperCase();
    }

    public String getUsingConcurrentMap(String key) {
        return concurrentMap.computeIfAbsent(key, k -> {
            cacheMissesWithConcurrentMap.incrementAndGet();
            return fetchDataFromDataSource(k);
        });
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentHashMapExample example = new ConcurrentHashMapExample();

        // Fetching the initially populated value
        System.out.println("Initial cache value: " + example.getUsingConcurrentMap("initial"));

        // Using ConcurrentHashMap
        System.out.println(example.getUsingConcurrentMap("hello"));
        System.out.println(example.getUsingConcurrentMap("hello"));

        System.out.println(example.getUsingConcurrentMap("world"));
        System.out.println(example.getUsingConcurrentMap("world"));

        System.out.println("Cache misses with ConcurrentHashMap: " + example.cacheMissesWithConcurrentMap.get());

        // Wait for 6 seconds (this does not affect ConcurrentHashMap as it does not have built-in expiry)
        Thread.sleep(6000);

        // Fetching again (this will be a cache hit as ConcurrentHashMap does not have built-in expiry)
        System.out.println("After 'expiration':");
        System.out.println(example.getUsingConcurrentMap("hello"));
        System.out.println("Cache misses with ConcurrentHashMap: " + example.cacheMissesWithConcurrentMap.get());
    }
}
