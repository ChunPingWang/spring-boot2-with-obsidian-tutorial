package com.example.tutorial.scheduling;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSchedulerTest {

    @Test
    void incrementsRunCountAndKeepsScheduledAnnotation() throws Exception {
        ReportScheduler scheduler = new ReportScheduler();
        scheduler.generateReport();
        assertThat(scheduler.getRunCount()).isEqualTo(1);

        Method method = ReportScheduler.class.getMethod("generateReport");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).isEqualTo("${demo.reports.delay:60000}");
    }
}
