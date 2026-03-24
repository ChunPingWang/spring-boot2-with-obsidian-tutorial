package com.example.modulith.order;

import com.example.modulith.catalog.CatalogService;
import com.example.modulith.customer.CustomerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final CustomerService customerService;
    private final CatalogService catalogService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderService(CustomerService customerService,
                        CatalogService catalogService,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.customerService = customerService;
        this.catalogService = catalogService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public BigDecimal placeOrder(Long customerId, Long productId, int quantity) {
        if (!customerService.exists(customerId)) {
            throw new IllegalArgumentException("Unknown customer: " + customerId);
        }
        BigDecimal total = catalogService.priceOf(productId).multiply(BigDecimal.valueOf(quantity));
        applicationEventPublisher.publishEvent(new OrderPlacedEvent(customerId, productId, total));
        return total;
    }
}
