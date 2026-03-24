package com.example.tutorial.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CheckoutServiceTest {

    private final CheckoutService checkoutService = new CheckoutService();

    @Test
    void writesInfoLog(CapturedOutput output) {
        assertThat(checkoutService.checkout("ORD-9")).isEqualTo("checked-out-ORD-9");
        assertThat(output).contains("processing order ORD-9");
    }
}
