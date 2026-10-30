package com.nbd.payments.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nbd.payments.payment.service.PaymentService;
import com.nbd.payments.admin.dto.AdminLoginRequest;
import com.nbd.payments.admin.dto.AdminMerchantResponse;
import com.nbd.payments.admin.dto.MerchantStatusRequest;
import com.nbd.payments.admin.service.AdminService;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.payment.dto.PaymentResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;



@RestController 
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final PaymentService paymentService;

    private  AdminController(
            AdminService adminService,
            PaymentService paymentService
            ){
        this.adminService = adminService;
        this.paymentService = paymentService;
    }

    @PostMapping("/login")
    public  ResponseEntity<Map<String, String>> login(
        @Valid @RequestBody AdminLoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ){
        adminService.authenticate(
            request.email(),
            request.password()
        );

        if (httpRequest.getSession(false)!= null) {
            httpRequest.changeSessionId();
        }else{
            httpRequest.getSession(true);
        }

        var authentication = 
                new UsernamePasswordAuthenticationToken(
                    "SYSTEM_ADMIN",
                    null,
                List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
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
        return  ResponseEntity.ok(
            Map.of(
                "Message", "Admin login successful",
                "role", "ADMIN"       
            )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(){
        return ResponseEntity.ok(
            Map.of( "role", "ADMIN",
            "status", "AUTHENTICATED"
        )
        
    );
    }
    @GetMapping("/merchants")
    public ResponseEntity<List<AdminMerchantResponse>> getMerchants(){
        return ResponseEntity.ok(
            adminService.getAllMerchant()
        );
    }

    @PatchMapping("/merchants/{merchantId}/status")
    public ResponseEntity<AdminMerchantResponse> updateStatus(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantStatusRequest request
    ) {

        AdminMerchantResponse response =
                adminService.updateMerchantStatus(
                        merchantId,
                        request.status()
                );

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(
            CsrfToken csrfToken
    ) {
        return ResponseEntity.ok(
                Map.of(
                        "headerName", csrfToken.getHeaderName(),
                        "token", csrfToken.getToken()
                )
        );
    }
    
    @GetMapping ("/transactions")
    public ResponseEntity<List<PaymentResponse>> transactions(){
        return  ResponseEntity.ok(
            paymentService.getAllTransactions()
        );
    }
}
