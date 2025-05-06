package com.rayfay.gira.config;

import com.rayfay.gira.security.CustomAuthenticationEntryPoint;
import com.rayfay.gira.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final CustomAuthenticationEntryPoint authenticationEntryPoint;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/login", "/api/auth/refresh-token",
                                                                "/h2-console/**",
                                                                "/swagger-ui/**", "/swagger-ui.html",
                                                                "/swagger-resources/**",
                                                                "/v3/api-docs/**", "/v3/api-docs.yaml",
                                                                "/webjars/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(authenticationEntryPoint));

                // 开发环境允许H2 Console访问
                if (System.getProperty("spring.profiles.active", "dev").equals("dev")) {
                        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
                }

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("*"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}