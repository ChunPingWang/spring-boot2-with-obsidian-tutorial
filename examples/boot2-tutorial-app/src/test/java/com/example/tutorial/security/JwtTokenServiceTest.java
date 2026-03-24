package com.example.tutorial.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void createsAndParsesToken() {
        JwtTokenService service = new JwtTokenService("01234567890123456789012345678901");
        service.init();
        String token = service.createToken("demo");
        assertThat(service.extractUsername(token)).isEqualTo("demo");
    }
}
