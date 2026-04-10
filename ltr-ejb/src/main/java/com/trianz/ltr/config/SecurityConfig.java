package com.trianz.ltr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Spring Security configuration for cloud-native authentication.
 * 
 * Replaces:
 * - EJB @RolesAllowed with Spring Security method-level security
 * - WebSphere JAAS with Spring Security
 * - SessionContext.getCallerPrincipal() with SecurityContextHolder
 * 
 * Cloud-native features:
 * - Stateless authentication (JWT-ready)
 * - Compatible with AWS Cognito, IAM, or custom OAuth2
 * - No session state (horizontal scaling friendly)
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    /**
     * Configure HTTP security with stateless session management.
     * In production, integrate with AWS Cognito or OAuth2 provider.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs
            .csrf().disable()
            
            // Stateless session management (no HTTP sessions)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // Authorization rules
            .authorizeRequests()
                // Health check endpoint - public
                .antMatchers("/health", "/actuator/health").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            .and()
            
            // HTTP Basic authentication (replace with JWT in production)
            .httpBasic();
        
        return http.build();
    }
}
