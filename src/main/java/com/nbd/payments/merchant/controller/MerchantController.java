package com.nbd.payments.merchant.controller;

import com.nbd.payments.merchant.dto.MerchantLoginRequest;
import com.nbd.payments.merchant.dto.MerchantLoginResponse;
import com.nbd.payments.merchant.dto.MerchantRegistrationRequest;
import com.nbd.payments.merchant.dto.MerchantRegistrationResponse;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.service.MerchantApiKeyService;
import com.nbd.payments.merchant.service.MerchantService;
import com.nbd.payments.payment.dto.PaymentResponse;
import com.nbd.payments.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController 
@RequestMapping("/api/v1/merchants")
public class MerchantController {

        private final MerchantService merchantService;
        private final MerchantApiKeyService merchantApiKeyService;
        private final PaymentService paymentService;

        @Value("${app.security.cookie-secure:false}")
        private boolean secureCookie;

        public MerchantController(
                MerchantService merchantService,
                MerchantApiKeyService merchantApiKeyService,
                PaymentService paymentService){
                this.merchantService = merchantService;
                this.merchantApiKeyService = merchantApiKeyService;
                this.paymentService = paymentService;
        }

        @PostMapping 
        public ResponseEntity<MerchantRegistrationResponse> registerMerchant(
                @Valid  @RequestBody  MerchantRegistrationRequest request,
                @CookieValue(
                        name = "NBD_REGISTRATION_AUTH",
                        required = false
                ) String authorizationToken,
                HttpServletResponse response
        ){
                Merchant merchant = merchantService.registerMerchant(
                        request.businessName(),
                        request.email(),
                        request.password(),
                        authorizationToken
                );

                ResponseCookie cookie = ResponseCookie
                        .from("NBD_REGISTRATION_AUTH", "")
                        .httpOnly(true)
                        .secure(secureCookie)
                        .path("/")
                        .sameSite("Lax")
                        .maxAge(Duration.ZERO)
                        .build();
                
                response.addHeader(
                        org.springframework.http.HttpHeaders.SET_COOKIE, 
                        cookie.toString()
                );

                MerchantRegistrationResponse registrationResponse = 
                new MerchantRegistrationResponse(
                        merchant.getMerchantId(),
                        merchant.getBusinessName(),
                        merchant.getEmail(),
                        merchant.getStatus(),
                        merchant.getCreatedAt()
                );

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(registrationResponse);
        }
                
        @PostMapping("/login")
        public ResponseEntity<MerchantLoginResponse> loginMerchant(
                @Valid @RequestBody MerchantLoginRequest request,
                HttpServletRequest httpRequest,
                jakarta.servlet.http.HttpServletResponse httpResponse
        ) {

                Merchant merchant = merchantService.loginMerchant(
                        request.email(),
                        request.password()
                );

                if (httpRequest.getSession(false) != null) {
                httpRequest.changeSessionId();
                } else {
                httpRequest.getSession(true);
                }

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                merchant.getMerchantId(),
                                null,
                                java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_MERCHANT")
                                )
                        );

                SecurityContext context =
                        SecurityContextHolder.createEmptyContext();

                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                new HttpSessionSecurityContextRepository()
                        .saveContext(
                        context,
                        httpRequest,
                        httpResponse
                        );

                return ResponseEntity.ok(
                        new MerchantLoginResponse(
                                merchant.getMerchantId(),
                                merchant.getBusinessName(),
                                merchant.getStatus(),
                                "Login successful"
                        )
                );
        }
                
        @PostMapping("/api-key")
        public ResponseEntity<Map<String, String>> generateApiKey(
                Authentication authentication
        ) {

                if (authentication == null ||
                        authentication.getAuthorities().stream()
                                .noneMatch(a ->
                                        a.getAuthority().equals("ROLE_MERCHANT")
                                )) {
                return ResponseEntity.status(403).build();
                }

                String merchantId = authentication.getName();

                String apiKey = merchantApiKeyService
                        .generateApiKey(merchantId);

                return ResponseEntity.ok(
                        Map.of(
                                "apiKey", apiKey,
                                "message", "Store this key securely. It will not be shown again."
                        )
                );
        }
        
        @GetMapping("/csrf")
        public ResponseEntity<Map<String, String>> csrf(
                org.springframework.security.web.csrf.CsrfToken csrfToken
        ) {
                return ResponseEntity.ok(
                        Map.of(
                                "headerName", csrfToken.getHeaderName(),
                                "token", csrfToken.getToken()
                        )
                );
        }
                
        @GetMapping("/transactions")
        public ResponseEntity<List<PaymentResponse>> transactions(
                Authentication authentication
        ) {

                Merchant merchant = merchantService.getActiveMerchant(
                        authentication.getName()
                );

                return ResponseEntity.ok(
                        paymentService.getMerchantTransactions(merchant)
                );
        }
                
        @GetMapping("/balance")
        public ResponseEntity<Map<String, Object>> balance(
                Authentication authentication
        ) {

                Merchant merchant = merchantService.getActiveMerchant(
                        authentication.getName()
                );

                Long balancePaise =
                        paymentService.getMerchantBalance(merchant);

                return ResponseEntity.ok(
                        Map.of(
                                "merchantId", merchant.getMerchantId(),
                                "currency", "INR",
                                "balancePaise", balancePaise
                        )
                );
        }
                
        @PostMapping("/refunds/{paymentId}")
        public ResponseEntity<PaymentResponse> refund(
                @PathVariable String paymentId,
                Authentication authentication
        ) {

                Merchant merchant = merchantService.getActiveMerchant(
                        authentication.getName()
                );

                return ResponseEntity.ok(
                        paymentService.refundPayment(
                                merchant,
                                paymentId
                        )
                );
        }
}
