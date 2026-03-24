package com.example.commerce.user;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class UserProfileService {

    private final Map<Long, UserProfile> users = Collections.singletonMap(1L, new UserProfile(1L, "alice"));

    public UserProfile findById(Long userId) {
        return users.get(userId);
    }
}
