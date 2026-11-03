package com.chamchamcham.domain.policy;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan("com.chamchamcham.domain")
@EnableJpaRepositories(basePackageClasses = PolicyProgramRepository.class)
@Import({PolicyProgramQueryRepositoryImpl.class, PolicyRecommendationQueryRepositoryImpl.class})
class PolicyJpaTestConfiguration {
}
