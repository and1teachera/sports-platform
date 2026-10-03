package com.sportsplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Deny-by-default API security.
 *
 * <p>Only the paths listed explicitly in the authorisation rules are permitted; every other
 * request is denied. New endpoints opt in by adding their own rule here.</p>
 *
 * <p>No form login, no HTTP basic, no OAuth2 resource server: those arrive with the slices that
 * actually need them. CSRF is disabled because the API is stateless and never relies on a cookie
 * session.</p>
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().denyAll()
                );
        return http.build();
    }
}
