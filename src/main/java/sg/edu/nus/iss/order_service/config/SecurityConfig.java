package sg.edu.nus.iss.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .xssProtection(xss -> xss.disable())  // Modern browsers have built-in XSS protection
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .cacheControl(cache -> cache.disable())  // Prevent sensitive info caching
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
            )
            .csrf(csrf -> csrf.disable())  // Since behind reverse proxy
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").denyAll()  // Block actuator endpoints
                .anyRequest().permitAll()
            );
            
        return http.build();
    }
} 