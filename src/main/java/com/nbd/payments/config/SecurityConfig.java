
package com.nbd.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
                    "/css/**",
                    "/js/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/health"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/merchants",
                    "/api/v1/merchants/login",
                    "/api/v1/admin/login"
                ).permitAll()

                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")

                .requestMatchers("/api/v1/merchants/**")
                .hasRole("MERCHANT")

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/payments"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/payments/checkout/*/pay"
                ).permitAll()

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

            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

            boolean anonymous =
                authentication == null ||
                !authentication.isAuthenticated() ||
                new AuthenticationTrustResolverImpl()
                    .isAnonymous(authentication);

            if (anonymous) {

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");

                response.getWriter().write(
                    "{\"status\":401,\"message\":\"Session expired. Please log in again.\"}"
                );

            } else {

                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");

                response.getWriter().write(
                    "{\"status\":403,\"message\":\"Access denied.\"}"
                );
            }
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
