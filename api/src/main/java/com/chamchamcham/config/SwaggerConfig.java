package com.chamchamcham.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(apiInfo()).servers(List.of(new Server().url("/"))).components(components())
            .addSecurityItem(new SecurityRequirement().addList("access-token"));
    }
    private Info apiInfo() {
        return new Info().title("ChamChamCham API").description("ChamChamCham Backend API Documentation").version("v1");
    }
    private Components components() {
        var securityScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER).name("Authorization");
        return new Components().addSecuritySchemes("access-token", securityScheme);
    }
}
