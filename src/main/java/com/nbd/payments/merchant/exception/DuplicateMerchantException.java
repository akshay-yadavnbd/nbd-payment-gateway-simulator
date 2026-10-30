package com.nbd.payments.merchant.exception;

public class DuplicateMerchantException extends RuntimeException{
    public  DuplicateMerchantException(String message){
        super(message);
    }
    
}
