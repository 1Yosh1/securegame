package com.unime.securegame.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("securegame-noreply@example.com");
        message.setTo(to);
        message.setSubject("SecureGame - Your Authentication Code");
        message.setText("Welcome to SecureGame! Your One-Time Password (OTP) is: " + otp + "\n\nThis code will expire in 5 minutes. Do not share it with anyone.");
        
        try {
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            // Never log a one-time code; authentication must fail closed if delivery fails.
            System.err.println("Failed to send authentication email: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
