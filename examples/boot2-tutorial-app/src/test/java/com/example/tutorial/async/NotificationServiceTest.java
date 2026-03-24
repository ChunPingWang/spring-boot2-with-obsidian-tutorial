package com.example.tutorial.async;

import com.example.tutorial.TutorialApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TutorialApplication.class)
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void executesOnNamedAsyncExecutor() throws Exception {
        String result = notificationService.sendReceipt("ORD-1").get(5, TimeUnit.SECONDS);
        assertThat(result).startsWith("notification-").endsWith(":ORD-1");
    }
}
