package com.example.modulith.order;

import java.math.BigDecimal;

public class OrderPlacedEvent {

    private final Long customerId;
    private final Long productId;
    private final BigDecimal totalAmount;

    public OrderPlacedEvent(Long customerId, Long productId, BigDecimal totalAmount) {
        this.customerId = customerId;
        this.productId = productId;
        this.totalAmount = totalAmount;
    }

    public Long customerId() {
        return customerId;
    }

    public Long productId() {
        return productId;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }
}
