package com.example.commerce.order;

import org.springframework.stereotype.Service;

@Service
public class OrderApplicationService {

    public OrderSummary placeOrder(OrderRequest request) {
        return new OrderSummary(
                request.getUserId(),
                request.getProductId(),
                request.getUnitPrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity()))
        );
    }
}
