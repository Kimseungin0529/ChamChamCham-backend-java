package com.chamchamcham.domain.pesticide;

import com.chamchamcham.domain.common.BaseTimeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PesticideQueryRepositoryTest.JpaConfiguration.class)
class PesticideQueryRepositoryTest {
    @Autowired TestEntityManager entityManager;
    @Autowired PesticideQueryRepository queryRepository;

    @Test
    void searchMatchesItemOrBrandWithoutTrimmingNonblankKeyword() {
        pesticide("살충제", "상품 A");
        pesticide("품목 B", "살충제 상품");
        pesticide("다른 품목", "다른 상품");
        entityManager.clear();

        assertThat(queryRepository.search(new PesticideQueryRepository.SearchCondition("살충제", null, 20)))
            .extracting(Pesticide::getItemName).containsExactlyInAnyOrder("살충제", "품목 B");
        assertThat(queryRepository.search(new PesticideQueryRepository.SearchCondition(" 살충제 ", null, 20))).isEmpty();
    }

    @Test
    void unicodeBlankKeywordDoesNotFilterRows() {
        pesticide("품목", "상품");
        entityManager.clear();
        assertThat(queryRepository.search(new PesticideQueryRepository.SearchCondition("\u00a0\u2007", null, 20))).hasSize(1);
    }

    @Test
    void equalBrandRowsContinueAcrossCursorPagesWithoutDuplicates() {
        pesticide("A", "같은 상품");
        pesticide("B", "같은 상품");
        pesticide("C", "같은 상품");
        entityManager.clear();
        var full = queryRepository.search(new PesticideQueryRepository.SearchCondition(null, null, 20));
        var first = queryRepository.search(new PesticideQueryRepository.SearchCondition(null, null, 1));
        var cursor = first.getFirst();
        var next = queryRepository.search(new PesticideQueryRepository.SearchCondition(null,
            new PesticideQueryRepository.Cursor(cursor.getBrandName(), cursor.getId()), 20));
        var ids = new ArrayList<UUID>();
        first.forEach(row -> ids.add(row.getId()));
        next.forEach(row -> ids.add(row.getId()));

        assertThat(full).extracting(row -> row.getId().toString()).isSorted();
        assertThat(ids).containsExactlyElementsOf(full.stream().map(Pesticide::getId).toList());
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void voiceCatalogMatchesExactCropNamesDeduplicatesAndLimitsResults() {
        var first = pesticide("A 품목", "A 상품");
        var second = pesticide("B 품목", "B 상품");
        var pest = persist(new Pest(null, "진딧물"));
        application(first, pest, "사과");
        application(first, pest, "배");
        application(second, pest, "사과");
        entityManager.clear();

        assertThat(queryRepository.findByCropNames(List.of("사과", "배"), 20)).containsExactly(
            new PesticideQueryRepository.VoiceCatalogRow("A 품목", "A 상품", "진딧물"),
            new PesticideQueryRepository.VoiceCatalogRow("B 품목", "B 상품", "진딧물"));
        assertThat(queryRepository.findByCropNames(List.of("사과"), 1)).hasSize(1);
        assertThat(queryRepository.findByCropNames(List.of(" 사과 "), 20)).isEmpty();
        assertThat(queryRepository.findByCropNames(List.of(), 20)).isEmpty();
    }

    private Pesticide pesticide(String item, String brand) {
        return persist(new Pesticide(null, item, brand, null, null, null, null, null, null));
    }

    private void application(Pesticide pesticide, Pest pest, String crop) {
        persist(new PesticideApplication(null, pesticide, pest, crop, null, null, null, null));
    }

    private <T extends BaseTimeEntity> T persist(T entity) {
        var now = LocalDateTime.of(2026, 7, 7, 9, 0);
        ReflectionTestUtils.setField(entity, "createdAt", now);
        ReflectionTestUtils.setField(entity, "updatedAt", now);
        return entityManager.persistAndFlush(entity);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories(basePackageClasses = PesticideRepository.class)
    @Import(PesticideQueryRepositoryImpl.class)
    static class JpaConfiguration {
    }
}
