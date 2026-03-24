package com.example.modulith.architecture;

import com.example.modulith.CommerceModulithApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules.of(CommerceModulithApplication.class).verify();
    }
}
