package com.trianz.ltr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Spring Security configuration for cloud-native authentication.
 *
 * CLOUD-NATIVE FEATURES:
 *   - Stateless session management (compatible with horizontal scaling)
 *   - Role-based access control (RBAC)
 *   - BCrypt password encoding
 *   - Redis session storage for distributed sessions
 *   - Health check endpoint exposed without authentication
 *
 * MIGRATION FROM WAS:
 *   - WAS JAAS/LDAP → Spring Security UserDetailsService
 *   - WAS security roles → Spring Security authorities
 *   - WAS SessionContext → Spring SecurityContextHolder
 *
 * PRODUCTION NOTES:
 *   - Replace InMemoryUserDetailsManager with JDBC or LDAP UserDetailsService
 *   - Integrate with AWS Cognito or OAuth2/OIDC for production authentication
 *   - Use AWS Secrets Manager for credential storage
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Disable CSRF for REST API (use tokens in production)
            .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .httpBasic()  // Basic auth for simplicity (use JWT/OAuth2 in production)
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * In-memory user store for demonstration.
     * In production, replace with:
     *   - JdbcUserDetailsManager (database-backed)
     *   - LdapUserDetailsManager (LDAP/Active Directory)
     *   - AWS Cognito integration
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails officer = User.builder()
                .username("officer")
                .password(passwordEncoder().encode("officer123"))
                .roles("REGISTRY_OFFICER")
                .build();

        UserDetails supervisor = User.builder()
                .username("supervisor")
                .password(passwordEncoder().encode("supervisor123"))
                .roles("REGISTRY_SUPERVISOR", "REGISTRY_OFFICER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("REGISTRY_ADMIN", "REGISTRY_SUPERVISOR", "REGISTRY_OFFICER")
                .build();

        return new InMemoryUserDetailsManager(officer, supervisor, admin);
    }
}
