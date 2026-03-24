package com.example.modulith.order;

import com.example.modulith.CommerceModulithApplication;
import com.example.modulith.notification.NotificationTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CommerceModulithApplication.class)
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotificationTracker notificationTracker;

    @BeforeEach
    void reset() {
        notificationTracker.reset();
    }

    @Test
    void publishesDomainEventWhenOrderPlaced() {
        BigDecimal total = orderService.placeOrder(1L, 101L, 2);
        assertThat(total).isEqualByComparingTo("4998.00");
        assertThat(notificationTracker.handledEvents()).hasSize(1);
        assertThat(notificationTracker.handledEvents().get(0).customerId()).isEqualTo(1L);
    }
}
