package com.nbd.payments.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity 
@Table (name = "merchants")

public class Merchant {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (
        name = "merchant_id",
        nullable = false,
        unique = true,
        length = 40
    )

    private String merchantId;

    @Column (
        name = "business_name",
        nullable = false,
        length = 150
    )
    private String businessName;

    @Column (
        name = "email",
        nullable = false,
        length = 254
    )
    private String email;

    @Column(
        name = "email_verified",
        nullable = false
    )
    private boolean emailVerified = false;

    @Column (
        name = "Status",
        nullable = false,
        length = 20
    )
    private String status = "PENDING";

    @Column (
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column (
        name = "updated_at",
        nullable = false
    )
    private OffsetDateTime updatedAt;


    protected  Merchant(){
        //Required by JPA
    }

    public Merchant(
        String merchantId,
        String businessName,
        String email
    ){
        this.merchantId = merchantId;
        this.businessName = businessName;
        this.email = email;
        this.status = "PENDING";
    }

    @jakarta.persistence.PrePersist 
    protected  void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @jakarta.persistence.PreUpdate 
    protected void onUpdate(){
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId(){
        return id;
    }
    public String getMerchantId(){
        return merchantId;
    }

    public String getBusinessName(){
        return businessName;
    }

    public String getEmail(){
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getStatus(){
        return status;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt(){
        return updatedAt;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public void setBusinessName(String businessName){
        this.businessName = businessName;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setStatus(String status){
        this.status = status;
    }
}
