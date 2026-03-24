package com.example.starter.autoconfigure;

public class GreetingService {

    private final GreetingProperties properties;

    public GreetingService(GreetingProperties properties) {
        this.properties = properties;
    }

    public String greet(String name) {
        return properties.getPrefix() + ", " + name + "!";
    }
}
