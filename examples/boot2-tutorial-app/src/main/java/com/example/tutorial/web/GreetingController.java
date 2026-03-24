package com.example.tutorial.web;

import com.example.starter.autoconfigure.GreetingService;
import com.example.tutorial.properties.TutorialProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    private final GreetingService greetingService;
    private final TutorialProperties tutorialProperties;

    public GreetingController(GreetingService greetingService, TutorialProperties tutorialProperties) {
        this.greetingService = greetingService;
        this.tutorialProperties = tutorialProperties;
    }

    @GetMapping("/greetings/{name}")
    public Map<String, String> greet(@PathVariable String name) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", greetingService.greet(name));
        response.put("tutorial", tutorialProperties.getName());
        return response;
    }
}
