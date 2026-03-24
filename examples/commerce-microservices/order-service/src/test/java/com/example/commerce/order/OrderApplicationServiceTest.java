package com.example.commerce.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApplicationServiceTest {

    private final OrderApplicationService orderApplicationService = new OrderApplicationService();

    @Test
    void calculatesOrderTotal() {
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setProductId(101L);
        request.setQuantity(2);
        request.setUnitPrice(new BigDecimal("2499.00"));

        OrderSummary summary = orderApplicationService.placeOrder(request);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo("4998.00");
    }
}
