package com.example.commerce.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogProductServiceTest {

    private final CatalogProductService catalogProductService = new CatalogProductService();

    @Test
    void returnsBuiltInProduct() {
        assertThat(catalogProductService.findById(101L).getName()).isEqualTo("Mechanical Keyboard");
    }
}
