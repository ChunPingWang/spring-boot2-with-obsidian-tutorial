package com.example.tutorial.aop;

import com.example.tutorial.TutorialApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TutorialApplication.class)
class ExecutionTimeAspectTest {

    @Autowired
    private MonitoredBusinessService monitoredBusinessService;

    @Autowired
    private MonitoringRecorder monitoringRecorder;

    @BeforeEach
    void reset() {
        monitoringRecorder.reset();
    }

    @Test
    void recordsInvokedMethodName() {
        assertThat(monitoredBusinessService.process("demo")).isEqualTo("processed-demo");
        assertThat(monitoringRecorder.getMethodNames()).containsExactly("process");
    }
}
