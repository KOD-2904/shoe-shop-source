package com.ttthinh.shoe_shop_basic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttthinh.shoe_shop_basic.auth.security.auth.RefreshTokenRateLimitFilter;
import com.ttthinh.shoe_shop_basic.auth.security.jwt.JwtAuthenticationFilter;
import com.ttthinh.shoe_shop_basic.auth.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/auth/login",
            "/auth/google",
            "/auth/token",
            "/auth/introspect",
            "/auth/log-out",
            "/auth/refreshToken",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/register"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpRequest,
            RefreshTokenRateLimitFilter refreshTokenRateLimitFilter
    ) throws Exception {
        httpRequest
                .cors(cors -> {
                })
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(refreshTokenRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/keepalive").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ghn/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/ghn").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/getProducts", "/products/getProduct", "/variants", "/variants/product/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/brand/getBrands", "/brand/getBrand", "/category/getCategorys", "/category/getCategory").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chatbot/message").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payment/vnpay-callback", "/api/payment/vnpay-ipn").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payment/vnpay-ipn").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        return httpRequest.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
