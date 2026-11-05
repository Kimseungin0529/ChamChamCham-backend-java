package com.chamchamcham.config;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.LinkedHashMap;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        var configuration = new LinkedHashMap<String, Object>();
        configuration.put("cloud_name", properties.cloudName());
        configuration.put("api_key", properties.apiKey());
        configuration.put("api_secret", properties.apiSecret());
        configuration.put("secure", properties.secure());
        return new Cloudinary(configuration);
    }
}
