package com.travelplatform.config;

import com.travelplatform.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // Swagger only in dev
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                // Flight status — tracking requires auth; single flight, search and simulation are public
                .requestMatchers(HttpMethod.GET, "/api/flight-status/tracked").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/flight-status/*/track").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/flight-status/*/untrack").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/flight-status/*/simulate").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flight-status/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flight-status/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flight-status/*/history").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flights/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/recommendations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/voyara/invitations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/addons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/prices/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/seats/map").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/holidays/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/trains/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/buses/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cabs/**").permitAll()

                // Payment endpoints
                .requestMatchers("/api/payments/webhook").permitAll()
                .requestMatchers("/api/payments/create-order").authenticated()
                .requestMatchers("/api/payments/verify").authenticated()

                // WebSocket
                .requestMatchers("/ws/**").permitAll()

                // Review moderation — require admin
                .requestMatchers("/api/reviews/moderation/**").hasRole("ADMIN")
                .requestMatchers("/api/reviews/*/moderate").hasRole("ADMIN")
                .requestMatchers("/api/reviews/eligibility").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                // Admin only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/rewards/admin/**").hasRole("ADMIN")

                // Rewards public config
                .requestMatchers("/api/rewards/config").permitAll()

                // Rewards customer endpoints - require authentication
                .requestMatchers("/api/rewards/**").authenticated()

                // Notifications - require authentication
                .requestMatchers("/api/notifications/**").authenticated()

                // Voyara differentiator endpoints — require authentication
                .requestMatchers("/api/voyara/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    public static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = List.of(
        "https://*.vercel.app",
        "https://voyara*.vercel.app",
        "https://voyara.com",
        "https://www.voyara.com",
        "https://*.up.railway.app",
        "http://localhost:*",
        "http://127.0.0.1:*"
    );

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000,https://*.vercel.app}")
    private String allowedOrigins;

    public static List<String> parseAllowedOriginPatterns(String rawOrigins) {
        Set<String> patterns = new LinkedHashSet<>(DEFAULT_ALLOWED_ORIGIN_PATTERNS);
        if (rawOrigins != null && !rawOrigins.isBlank()) {
            for (String raw : rawOrigins.split(",")) {
                String cleaned = raw.trim()
                        .replaceAll("^[\"']+|[\"']+$", "")
                        .replaceAll("/+$", "")
                        .trim();
                if (!cleaned.isEmpty()) {
                    patterns.add(cleaned);
                }
            }
        }
        return new ArrayList<>(patterns);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = parseAllowedOriginPatterns(allowedOrigins);
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
