package com.chamchamcham.config;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionRagConfigurationTest {
    private final List<PropertySource<?>> productionPropertySources = new YamlPropertySourceLoader()
            .load("application-prod.yml", new ClassPathResource("application-prod.yml"));

    ProductionRagConfigurationTest() throws java.io.IOException { }

    @Test
    @DisplayName("production vector table defaults to Spring AI vector store")
    void productionVectorTableDefaultsToSpringAiVectorStore() {
        assertEquals("vector_store", resolver(null).getProperty("spring.ai.vectorstore.pgvector.table-name"));
    }

    @Test
    @DisplayName("production vector table can be overridden by environment")
    void productionVectorTableCanBeOverriddenByEnvironment() {
        var resolver = resolver(new MapPropertySource("ragOverride", Map.of("RAG_VECTOR_TABLE", "vector_store_test")));
        assertEquals("vector_store_test", resolver.getProperty("spring.ai.vectorstore.pgvector.table-name"));
    }

    @Test
    @DisplayName("production validates the manually managed vector schema")
    void productionValidatesTheManuallyManagedVectorSchema() {
        assertEquals(true, resolver(null).getProperty("spring.ai.vectorstore.pgvector.schema-validation", Boolean.class));
    }

    private PropertySourcesPropertyResolver resolver(MapPropertySource override) {
        var propertySources = new MutablePropertySources();
        if (override != null) propertySources.addFirst(override);
        productionPropertySources.forEach(propertySources::addLast);
        return new PropertySourcesPropertyResolver(propertySources);
    }
}
