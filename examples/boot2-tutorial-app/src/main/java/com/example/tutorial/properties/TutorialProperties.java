package com.example.tutorial.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tutorial")
public class TutorialProperties {

    private String name = "Spring Boot 2 Tutorial";
    private String description = "Runnable examples";
    private final Contact contact = new Contact();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Contact getContact() {
        return contact;
    }

    public static class Contact {
        private String email = "team@example.com";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
