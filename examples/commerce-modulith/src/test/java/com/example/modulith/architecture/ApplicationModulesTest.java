package com.example.modulith.architecture;

import com.example.modulith.CommerceModulithApplication;
import org.junit.jupiter.api.Test;
import org.moduliths.model.Modules;

class ApplicationModulesTest {

    @Test
    void verifiesModuleStructure() {
        Modules.of(CommerceModulithApplication.class).verify();
    }
}
