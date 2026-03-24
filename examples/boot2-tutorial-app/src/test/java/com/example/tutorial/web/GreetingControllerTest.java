package com.example.tutorial.web;

import com.example.tutorial.TutorialApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TutorialApplication.class)
@AutoConfigureMockMvc
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsGreetingFromCustomStarter() throws Exception {
        mockMvc.perform(get("/api/greetings/Copilot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Copilot!"))
                .andExpect(jsonPath("$.tutorial").value("Spring Boot 2 Tutorial"));
    }
}
