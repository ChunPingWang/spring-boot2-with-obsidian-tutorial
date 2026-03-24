package com.example.tutorial.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CachedCatalogService {

    private final AtomicInteger lookupCount = new AtomicInteger();

    @Cacheable("productLabels")
    public String findLabel(Long productId) {
        lookupCount.incrementAndGet();
        return "product-" + productId;
    }

    public int getLookupCount() {
        return lookupCount.get();
    }

    public void reset() {
        lookupCount.set(0);
    }
}
