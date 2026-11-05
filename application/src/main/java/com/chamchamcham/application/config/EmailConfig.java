package com.chamchamcham.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;
import java.util.Objects;

@Configuration
public class EmailConfig {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean auth;
    private final boolean starttlsEnable;
    private final boolean starttlsRequired;
    private final int connectionTimeout;
    private final int timeout;
    private final int writeTimeout;

    public EmailConfig(@Value("${spring.mail.host}") String host, @Value("${spring.mail.port}") int port, @Value("${spring.mail.username}") String username, @Value("${spring.mail.password}") String password, @Value("${spring.mail.properties.mail.smtp.auth}") boolean auth, @Value("${spring.mail.properties.mail.smtp.starttls.enable}") boolean starttlsEnable, @Value("${spring.mail.properties.mail.smtp.starttls.required}") boolean starttlsRequired, @Value("${spring.mail.properties.mail.smtp.connectiontimeout}") int connectionTimeout, @Value("${spring.mail.properties.mail.smtp.timeout}") int timeout, @Value("${spring.mail.properties.mail.smtp.writetimeout}") int writeTimeout) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.auth = auth;
        this.starttlsEnable = starttlsEnable;
        this.starttlsRequired = starttlsRequired;
        this.connectionTimeout = connectionTimeout;
        this.timeout = timeout;
        this.writeTimeout = writeTimeout;
    }
    @Bean public JavaMailSender javaMailSender() {
        var sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        sender.setJavaMailProperties(getMailProperties());
        return sender;
    }
    private Properties getMailProperties() {
        var properties = new Properties();
        properties.put("mail.smtp.auth", Boolean.toString(auth));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(starttlsEnable));
        properties.put("mail.smtp.starttls.required", Boolean.toString(starttlsRequired));
        properties.put("mail.smtp.connectiontimeout", Integer.toString(connectionTimeout));
        properties.put("mail.smtp.timeout", Integer.toString(timeout));
        properties.put("mail.smtp.writetimeout", Integer.toString(writeTimeout));
        return properties;
    }
}
