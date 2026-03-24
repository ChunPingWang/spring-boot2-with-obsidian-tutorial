package com.example.modulith.catalog;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class CatalogService {

    private final Map<Long, BigDecimal> prices = Map.of(101L, new BigDecimal("2499.00"));

    public BigDecimal priceOf(Long productId) {
        return prices.get(productId);
    }
}
