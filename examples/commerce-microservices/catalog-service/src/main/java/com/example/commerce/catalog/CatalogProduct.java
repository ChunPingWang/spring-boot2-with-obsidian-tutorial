package com.example.commerce.catalog;

import java.math.BigDecimal;

public class CatalogProduct {

    private final Long id;
    private final String name;
    private final BigDecimal price;

    public CatalogProduct(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
