package com.evbooking.config;

import com.evbooking.security.CustomUserDetailsService;
import com.evbooking.security.SessionAuthEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * spring security config using the SecurityFilterChain bean model.
 * session based auth only, no jwt.
 * spring session jdbc stores sessions in postgres so all heroku dynos
 * share the same session state, wich is needed when scaling up.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final SessionAuthEntryPoint authEntryPoint;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          SessionAuthEntryPoint authEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // api docs restricted to admins
                .requestMatchers("/docs.html", "/openapi.yaml").hasRole("ADMIN")
                // static files, anyone can access these
                .requestMatchers("/", "/index.html", "/login.html", "/register.html",
                                 "/dashboard.html", "/stations.html",
                                 "/bookings.html", "/admin.html",
                                 "/css/**", "/js/**", "/favicon.ico").permitAll()
                // station reads are public so the map works without logging in
                .requestMatchers(HttpMethod.GET, "/api/stations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/connectors/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/slots/**").permitAll()
                // auth is open to everyone
                .requestMatchers("/api/auth/**").permitAll()
                // admin only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // bookings need at least driver role
                .requestMatchers("/api/bookings/**").hasAnyRole("DRIVER", "ADMIN")
                // everything else needs authentication
                .anyRequest().authenticated()
            )
            // csrf is disabled, the api is only called from same-origin js
            // and uses SameSite=Lax session cookies so its fine
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler((request, response, denied) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("text/html")) {
                        response.sendRedirect("/index.html");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to perform this action.\"}");
                    }
                })
            )
            .sessionManagement(session -> session
                // change session id on login to prevent session fixation
                .sessionFixation().changeSessionId()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
