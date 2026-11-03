package com.chamchamcham.domain.common;

import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import kotlin.UninitializedPropertyAccessException;
import org.hibernate.Hibernate;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = BaseTimeEntityJpaTest.JpaConfiguration.class)
class BaseTimeEntityJpaTest {
    @Autowired TestEntityManager entityManager;

    @Test
    void unsetAuditTimestampsKeepTheExistingLateinitFailureContract() {
        var entity = new BaseTimeEntity() { };
        assertThatThrownBy(entity::getCreatedAt).isInstanceOf(UninitializedPropertyAccessException.class);
        assertThatThrownBy(entity::getUpdatedAt).isInstanceOf(UninitializedPropertyAccessException.class);
    }

    @Test
    void auditTimestampsSurvivePersistenceRoundTrip() {
        var program = persistProgram();
        entityManager.clear();
        var loaded = entityManager.find(PolicyProgram.class, program.getId());

        assertThat(loaded.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 9, 0));
        assertThat(loaded.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 7, 10, 0));
    }

    @Test
    void commonAuditAccessorsAllowAnEntityReferenceToStayLazy() {
        var program = persistProgram();
        entityManager.clear();

        var reference = entityManager.getEntityManager().getReference(PolicyProgram.class, program.getId());

        assertThat(Hibernate.isInitialized(reference)).isFalse();
        assertThat(reference.getTitle()).isEqualTo("정책");
        assertThat(Hibernate.isInitialized(reference)).isTrue();
    }

    private PolicyProgram persistProgram() {
        var program = new PolicyProgram("정책", "본문", "전국", null);
        ReflectionTestUtils.setField(program, "createdAt", LocalDateTime.of(2026, 7, 7, 9, 0));
        ReflectionTestUtils.setField(program, "updatedAt", LocalDateTime.of(2026, 7, 7, 10, 0));
        return entityManager.persistAndFlush(program);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories(basePackageClasses = PolicyProgramRepository.class)
    static class JpaConfiguration {
    }
}
