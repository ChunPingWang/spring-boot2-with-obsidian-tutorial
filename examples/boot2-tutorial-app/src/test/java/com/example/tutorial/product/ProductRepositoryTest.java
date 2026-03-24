package com.example.tutorial.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void savesAndLoadsProduct() {
        Product product = productRepository.save(new Product("Keyboard", new BigDecimal("1299.00")));
        assertThat(productRepository.findById(product.getId())).isPresent();
    }
}
