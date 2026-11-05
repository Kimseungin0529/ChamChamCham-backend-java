package com.chamchamcham.application.config;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;

class AuthInfrastructureConfigTest {
    @Test void clockAndMailPropertiesKeepTheirConfiguredValuesWithoutConnecting() {
        assertEquals("Asia/Seoul", new ClockConfig().clock().getZone().getId());
        var sender = (JavaMailSenderImpl) new EmailConfig("localhost", 2525, "fixture", "fixture",
            true, false, true, 100, 200, 300).javaMailSender();
        assertEquals("localhost", sender.getHost());
        assertEquals(2525, sender.getPort());
        assertEquals("fixture", sender.getUsername());
        assertEquals("UTF-8", sender.getDefaultEncoding());
        var properties = sender.getJavaMailProperties();
        assertEquals(6, properties.size());
        assertEquals("true", properties.getProperty("mail.smtp.auth"));
        assertEquals("false", properties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", properties.getProperty("mail.smtp.starttls.required"));
        assertEquals("100", properties.getProperty("mail.smtp.connectiontimeout"));
        assertEquals("200", properties.getProperty("mail.smtp.timeout"));
        assertEquals("300", properties.getProperty("mail.smtp.writetimeout"));
    }
}
