package com.example.modulith.catalog;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Service
public class CatalogService {

    private final Map<Long, BigDecimal> prices = Collections.singletonMap(101L, new BigDecimal("2499.00"));

    public BigDecimal priceOf(Long productId) {
        return prices.get(productId);
    }
}
