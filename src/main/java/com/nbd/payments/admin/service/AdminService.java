package com.nbd.payments.admin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nbd.payments.admin.dto.AdminMerchantResponse;
import com.nbd.payments.admin.dto.MerchantStatus;
import com.nbd.payments.merchant.entity.Merchant;
import com.nbd.payments.merchant.repository.MerchantRepository;

@Service 
public class AdminService {
    
    private final String adminEmail;
    private final String adminPasswordHash;
    private final PasswordEncoder passwordEncoder;
    private  final MerchantRepository merchantRepository;

    public AdminService(
        @Value ("${nbd.admin.email}") String adminEmail,
        @Value ("${nbd.admin.password-hash}") String adminPasswordHash,
        PasswordEncoder passwordEncoder,
        MerchantRepository merchantRepository
    ){
        this.adminEmail = adminEmail;
        this.adminPasswordHash = adminPasswordHash;
        this.passwordEncoder = passwordEncoder;
        this.merchantRepository = merchantRepository;
    }

    public void authenticate (String email, String password){
        if (email == null || 
            email.isBlank() || 
            password == null ||
            password.isBlank()
            ) {
            throw new BadCredentialsException(
                "Invalid admin email or password"
            );
            
        }

        boolean emailMatches = 
                    adminEmail.equalsIgnoreCase(email.trim());
        
        boolean passwordMatches =
                    passwordEncoder.matches(
                        password,
                        adminPasswordHash
                    );

        if (!emailMatches || !passwordMatches) {
            throw new BadCredentialsException(
                "Invalid admin email or password"
            );
        }
    }
    @Transactional (readOnly = true)
    public List<AdminMerchantResponse> getAllMerchant(){
        return merchantRepository.findAll()
                    .stream()
                    .map(AdminMerchantResponse::from)
                    .toList();
    }

    @Transactional 
    public AdminMerchantResponse updateMerchantStatus(
        String merchantId,
        MerchantStatus newStatus
    ){
        Merchant merchant = merchantRepository
                        .findByMerchantId(merchantId)
                        .orElseThrow(() ->
                                new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.NOT_FOUND,
                                        "Merchant not found"
                                )
                        );
        merchant.setStatus(newStatus.name());

                        
        Merchant updMerchant= 
                        merchantRepository.saveAndFlush(merchant);
        
        return AdminMerchantResponse.from(updMerchant);
    }
}
