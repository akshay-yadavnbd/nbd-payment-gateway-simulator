package com.nbd.payments.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
    @NotBlank (message = "Email can't be blank")
    @Email 
    String email,

    @NotBlank (message = "Password can't be blank")
    String password
) {}
