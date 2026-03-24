package com.example.commerce.order;

import java.math.BigDecimal;

public class OrderSummary {

    private final Long userId;
    private final Long productId;
    private final BigDecimal totalAmount;

    public OrderSummary(Long userId, Long productId, BigDecimal totalAmount) {
        this.userId = userId;
        this.productId = productId;
        this.totalAmount = totalAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
