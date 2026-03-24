package com.example.tutorial.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    @Async("notificationExecutor")
    public CompletableFuture<String> sendReceipt(String orderId) {
        return CompletableFuture.completedFuture(Thread.currentThread().getName() + ":" + orderId);
    }
}
