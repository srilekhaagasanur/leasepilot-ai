package com.leasepilot.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

// Who can call what.
// Logging in creates a session; the browser keeps it in an HttpOnly cookie (JavaScript can't read it).
// Because a cookie is sent automatically, every POST also needs a CSRF token: the backend puts it in
// the XSRF-TOKEN cookie and the frontend echoes it back in the X-XSRF-TOKEN header (see api.js).
// A different website can't read our cookie, so it can't forge requests on the user's behalf.
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // /error is where Spring renders error messages (e.g. "Wrong email or password")
                        .requestMatchers("/error", "/api/health", "/api/auth/**", "/api/demo/**").permitAll()
                        .requestMatchers("/api/manager/**", "/api/ai/**").hasRole("MANAGER")
                        .requestMatchers("/api/tenant/**").hasRole("TENANT")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.spa())
                // This is a JSON API: answer 401 instead of redirecting to a login page
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
        return http.build();
    }

    // BCrypt is slow on purpose, so stolen hashes are hard to crack
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
