package com.example.modulith.notification;

import com.example.modulith.order.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class NotificationTracker {

    private final List<OrderPlacedEvent> handledEvents = new ArrayList<>();

    @EventListener
    public void on(OrderPlacedEvent event) {
        handledEvents.add(event);
    }

    public List<OrderPlacedEvent> handledEvents() {
        return Collections.unmodifiableList(handledEvents);
    }

    public void reset() {
        handledEvents.clear();
    }
}
