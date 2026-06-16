package com.trianz.ltr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfiguration - Spring Security configuration for cloud-native deployment.
 * 
 * CLOUD-NATIVE MIGRATION:
 *   - Replaces WebSphere JAAS/LDAP security with Spring Security
 *   - Supports AWS Cognito, IAM, or custom authentication providers
 *   - Stateless session management with Redis-backed sessions
 * 
 * SECURITY ROLES:
 *   - REGISTRY_OFFICER: Data entry and basic operations
 *   - REGISTRY_SUPERVISOR: Approve/reject transfers
 *   - REGISTRY_ADMIN: Full system access
 * 
 * FUTURE ENHANCEMENTS:
 *   - Integrate with AWS Cognito for user authentication
 *   - Use AWS IAM roles for service-to-service authentication
 *   - Implement OAuth2/JWT for API security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // CSRF protection (disable for REST APIs, enable for form-based)
            .csrf().disable()
            
            // Authorization rules
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/health", "/actuator/**").permitAll()
                
                // Title search - authenticated users only
                .antMatchers("GET", "/api/titles/**")
                    .hasAnyRole("REGISTRY_OFFICER", "REGISTRY_SUPERVISOR", "REGISTRY_ADMIN")
                
                // Title mutations - requires officer role or higher
                .antMatchers("POST", "/api/titles/**")
                    .hasAnyRole("REGISTRY_OFFICER", "REGISTRY_SUPERVISOR", "REGISTRY_ADMIN")
                .antMatchers("PUT", "/api/titles/**")
                    .hasAnyRole("REGISTRY_OFFICER", "REGISTRY_SUPERVISOR", "REGISTRY_ADMIN")
                
                // Transfer operations - authenticated users
                .antMatchers("/api/transfers/**")
                    .hasAnyRole("REGISTRY_OFFICER", "REGISTRY_SUPERVISOR", "REGISTRY_ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            .and()
            
            // Form-based login (for backward compatibility)
            .formLogin()
                .loginPage("/pages/login.jsp")
                .failureUrl("/pages/login-error.jsp")
                .permitAll()
            .and()
            
            // Logout configuration
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/pages/login.jsp")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            .and()
            
            // Session management (Redis-backed)
            .sessionManagement()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false);
        
        // HTTPS enforcement (handled by AWS ALB/CloudFront in production)
        // Uncomment for local HTTPS testing:
        // http.requiresChannel().anyRequest().requiresSecure();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
