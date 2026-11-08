package com.chamchamcham.config;

import com.chamchamcham.api.security.CustomAccessDeniedHandler;
import com.chamchamcham.api.security.CustomAuthenticationEntryPoint;
import com.chamchamcham.api.security.JwtAuthenticationFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MDCLoggingFilter mdcLoggingFilter;
    private final List<String> corsAllowedOrigins;
    private static final String[] PUBLIC_ENDPOINTS = {
        "/swagger-ui/**", "/v3/api-docs/**",
        "/api/v1/auth/email/send-code", "/api/v1/auth/email/verify-code", "/api/v1/auth/signup",
        "/api/v1/auth/login", "/api/v1/auth/kakao/login", "/api/v1/auth/apple/login", "/api/v1/auth/naver/login",
        "/api/v1/auth/reissue", "/error"
    };

    public SecurityConfig(CustomAccessDeniedHandler accessDeniedHandler, CustomAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAuthenticationFilter jwtAuthenticationFilter, MDCLoggingFilter mdcLoggingFilter,
                          @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}") String allowedCorsOrigins) {
        this.accessDeniedHandler = Objects.requireNonNull(accessDeniedHandler, "accessDeniedHandler");
        this.authenticationEntryPoint = Objects.requireNonNull(authenticationEntryPoint, "authenticationEntryPoint");
        this.jwtAuthenticationFilter = Objects.requireNonNull(jwtAuthenticationFilter, "jwtAuthenticationFilter");
        this.mdcLoggingFilter = Objects.requireNonNull(mdcLoggingFilter, "mdcLoggingFilter");
        this.corsAllowedOrigins = parseCorsOrigins(Objects.requireNonNull(allowedCorsOrigins, "allowedCorsOrigins"));
        if (corsAllowedOrigins.isEmpty()) throw new IllegalArgumentException("app.cors.allowed-origins must contain at least one origin");
        if (corsAllowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("Credentialed CORS must use explicit origins, not wildcards");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(it -> it.disable()).formLogin(it -> it.disable()).httpBasic(it -> it.disable())
            .cors(it -> it.configurationSource(request -> {
                var configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(corsAllowedOrigins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                return configuration;
            }))
            .sessionManagement(it -> it.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(it -> it.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(it -> it.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/community/posts", "/api/v1/community/posts/*", "/api/v1/community/posts/*/comments").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN").anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(mdcLoggingFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<MDCLoggingFilter> loggingFilterRegistration(MDCLoggingFilter mdcLoggingFilter) {
        var registration = new FilterRegistrationBean<MDCLoggingFilter>();
        registration.setFilter(mdcLoggingFilter);
        registration.setEnabled(false);
        registration.addUrlPatterns("/*");
        return registration;
    }

    private List<String> parseCorsOrigins(String value) {
        var origins = new ArrayList<String>();
        for (String part : value.split(",", -1)) {
            int start = 0;
            int end = part.length();
            while (start < end && whitespace(part.charAt(start))) start++;
            while (end > start && whitespace(part.charAt(end - 1))) end--;
            var origin = part.substring(start, end);
            if (!origin.isEmpty()) origins.add(origin);
        }
        return origins;
    }

    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
