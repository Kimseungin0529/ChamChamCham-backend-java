package com.chamchamcham.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.*;
import org.springframework.core.io.ClassPathResource;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
class LocalServerBindingConfigurationTest {
    private final List<PropertySource<?>> localProfilePropertySources = new YamlPropertySourceLoader()
        .load("application-local.yml", new ClassPathResource("application-local.yml"));
    LocalServerBindingConfigurationTest() throws java.io.IOException {}
    @Test @DisplayName("local 서버는 기본적으로 이 컴퓨터에서만 접근할 수 있다")
    void localServerDefaultsToThisComputerOnly() { assertEquals("127.0.0.1", resolver(null).getProperty("server.address")); }
    @Test @DisplayName("환경 변수로 외부 접근 주소를 명시할 수 있다")
    void environmentVariableCanOverrideServerAddress() {
        assertEquals("0.0.0.0", resolver(new MapPropertySource("serverAddressOverride", Map.of("SERVER_ADDRESS", "0.0.0.0"))).getProperty("server.address"));
    }
    private PropertySourcesPropertyResolver resolver(MapPropertySource override) {
        var sources = new MutablePropertySources();
        if (override != null) sources.addFirst(override);
        localProfilePropertySources.forEach(sources::addLast);
        return new PropertySourcesPropertyResolver(sources);
    }
}
