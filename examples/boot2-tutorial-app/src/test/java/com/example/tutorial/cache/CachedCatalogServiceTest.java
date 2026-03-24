package com.example.tutorial.cache;

import com.example.tutorial.TutorialApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TutorialApplication.class)
class CachedCatalogServiceTest {

    @Autowired
    private CachedCatalogService cachedCatalogService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void reset() {
        cachedCatalogService.reset();
        cacheManager.getCache("productLabels").clear();
    }

    @Test
    void cachesRepeatedLookups() {
        assertThat(cachedCatalogService.findLabel(7L)).isEqualTo("product-7");
        assertThat(cachedCatalogService.findLabel(7L)).isEqualTo("product-7");
        assertThat(cachedCatalogService.getLookupCount()).isEqualTo(1);
    }
}
