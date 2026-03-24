package com.example.tutorial.properties;

import com.example.tutorial.TutorialApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TutorialApplication.class, properties = {
        "tutorial.name=Doc Driven Tutorial",
        "tutorial.contact.email=docs@example.com"
})
class TutorialPropertiesTest {

    @Autowired
    private TutorialProperties tutorialProperties;

    @Test
    void bindsExternalizedConfiguration() {
        assertThat(tutorialProperties.getName()).isEqualTo("Doc Driven Tutorial");
        assertThat(tutorialProperties.getContact().getEmail()).isEqualTo("docs@example.com");
    }
}
