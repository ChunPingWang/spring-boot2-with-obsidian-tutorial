package com.example.tutorial.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReportScheduler {

    private final AtomicInteger runCount = new AtomicInteger();

    @Scheduled(fixedDelayString = "${demo.reports.delay:60000}")
    public void generateReport() {
        runCount.incrementAndGet();
    }

    public int getRunCount() {
        return runCount.get();
    }

    public void reset() {
        runCount.set(0);
    }
}
