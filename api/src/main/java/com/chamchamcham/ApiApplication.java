package com.chamchamcham;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.voice.VoiceSessionProperties;
import com.chamchamcham.config.OpenClawProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.chamchamcham")
@EnableConfigurationProperties({RagProperties.class, OpenClawProperties.class, VoiceSessionProperties.class})
public class ApiApplication {
    public static void main(String[] args) { SpringApplication.run(ApiApplication.class, args); }
}
