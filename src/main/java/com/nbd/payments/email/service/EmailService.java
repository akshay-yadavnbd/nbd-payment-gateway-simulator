package com.nbd.payments.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email for NBD Payments");
        message.setText(
                "Hello,\n\n" +
                "Thank you for choosing NBD Paymnets. \n\n" + 
                "Please verify your email address by clicking the link below:\n\n" +
                verificationLink + "\n\n" +
                "This verification link expires in 30 minutes.\n\n" +
                "If you did not request this, you can safely ignore this email.\n\n" +
                "Regards,\n" +
                "NBD Payments"
        );

        mailSender.send(message);
    }
}