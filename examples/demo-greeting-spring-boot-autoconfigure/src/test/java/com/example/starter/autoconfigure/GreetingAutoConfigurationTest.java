package com.example.starter.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GreetingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GreetingAutoConfiguration.class));

    @Test
    void createsGreetingServiceWithDefaultPrefix() {
        contextRunner.run(context -> {
            GreetingService service = context.getBean(GreetingService.class);
            assertThat(service.greet("Spring")).isEqualTo("Hello, Spring!");
        });
    }

    @Test
    void bindsCustomPrefixFromProperties() {
        contextRunner
                .withPropertyValues("demo.greeting.prefix=Hi")
                .run(context -> {
                    GreetingService service = context.getBean(GreetingService.class);
                    assertThat(service.greet("Boot")).isEqualTo("Hi, Boot!");
                });
    }
}
