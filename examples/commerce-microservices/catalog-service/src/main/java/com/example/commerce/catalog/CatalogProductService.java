package com.example.commerce.catalog;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Service
public class CatalogProductService {

    private final Map<Long, CatalogProduct> products = Collections.singletonMap(
            101L, new CatalogProduct(101L, "Mechanical Keyboard", new BigDecimal("2499.00"))
    );

    public CatalogProduct findById(Long productId) {
        return products.get(productId);
    }
}
