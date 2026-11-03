package com.chamchamcham.domain.crop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = CropRepositoryTest.JpaConfiguration.class)
class CropRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CropRepository cropRepository;

    @Test
    void keywordSearchMatchesCropNameIgnoringCase() {
        persistCrop("황기", 1);
        persistCrop("인삼", 2);
        entityManager.flush();
        entityManager.clear();

        var result = cropRepository.findTop9ByNameContainingIgnoreCaseOrderByNameAsc("황");

        assertThat(result).extracting(Crop::getName).containsExactly("황기");
    }

    @Test
    void keywordSearchLimitsToTop9OrderedByNameAscending() {
        for (int i = 1; i <= 12; i++) {
            persistCrop("작물%02d".formatted(i), 100 + i);
        }
        entityManager.flush();
        entityManager.clear();

        var result = cropRepository.findTop9ByNameContainingIgnoreCaseOrderByNameAsc("작물");

        assertThat(result).hasSize(9);
        assertThat(result).extracting(Crop::getName).isSorted();
    }

    private Crop persistCrop(String name, int externalNo) {
        var crop = new Crop(null, externalNo, name, CropUsePartCategory.ROOT_BARK);
        var now = LocalDateTime.of(2026, 7, 16, 12, 0);
        ReflectionTestUtils.setField(crop, "createdAt", now);
        ReflectionTestUtils.setField(crop, "updatedAt", now);
        return entityManager.persistFlushFind(crop);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories("com.chamchamcham.domain")
    static class JpaConfiguration {
    }
}
