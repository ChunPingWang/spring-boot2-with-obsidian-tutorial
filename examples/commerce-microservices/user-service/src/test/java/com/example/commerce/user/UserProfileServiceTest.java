package com.example.commerce.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileServiceTest {

    private final UserProfileService userProfileService = new UserProfileService();

    @Test
    void returnsBuiltInUser() {
        assertThat(userProfileService.findById(1L).getUsername()).isEqualTo("alice");
    }
}
