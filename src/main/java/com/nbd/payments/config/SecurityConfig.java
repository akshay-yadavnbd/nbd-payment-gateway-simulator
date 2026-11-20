
package com.nbd.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import org.springframework.http.HttpStatus;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
                .ignoringRequestMatchers(
                    "/api/v1/merchants",
                    "/api/v1/merchants/login",
                    "/api/v1/admin/login",
                    "/api/v1/payments",
                    "/api/v1/payments/checkout/*/pay"
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.GET,
                    "/payment.html",
                    "/merchant.html",
                    "/admin.html",
                    "/email-verification.html",
                    "/css/**",
                    "/js/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/health",
                    "/api/v1/email-verification/verify",
                    "/api/v1/email-verification/registration-status",
                    "/api/v1/payments/checkout/*"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/merchants",
                    "/api/v1/merchants/login",
                    "/api/v1/admin/login",
                    "/api/v1/payments/checkout/*/pay",
                    "/api/v1/payments",
                    "/api/v1/email-verification/clear-authorization",
                    "/api/v1/email-verification/send",
                    "/api/v1/email-verification/request",
                    "/api/v1/auth/merchant/forgot-password",
                    "/api/v1/auth/merchant/reset-password"
                ).permitAll()

                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/merchants/csrf"
                ).permitAll()

                .requestMatchers("/api/v1/merchants/**")
                .hasRole("MERCHANT")

                .requestMatchers(HttpMethod.GET, "/api/v1/payments/*").permitAll()

                .anyRequest().authenticated()
            )
            .securityContext(context -> context
                .securityContextRepository(
                    new HttpSessionSecurityContextRepository()
                )
            )

            .exceptionHandling(exception -> exception

            .authenticationEntryPoint((request, response, authException) -> {

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");

                response.getWriter().write(
                    "{\"status\":401,\"message\":\"Session expired. Please log in again.\"}"
                );
            })

            .accessDeniedHandler((request, response, accessDeniedException) -> {

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                response.getWriter().write(
                    "{\"status\":403,\"message\":\"Access denied. You do not have permission to perform this action.\"}"
                );
            })
        )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

            http.logout(logout -> logout
            .logoutUrl("/logout")
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> {
                response.setStatus(HttpStatus.OK.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"message\":\"Logout successful\"}"
                );
            })
        );

        return http.build();
    }
}
