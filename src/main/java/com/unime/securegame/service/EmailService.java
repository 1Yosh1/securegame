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

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("securegame-noreply@example.com");
        message.setTo(to);
        message.setSubject("SecureGame - Your Authentication Code");
        message.setText("Welcome to SecureGame! Your One-Time Password (OTP) is: " + otp + "\n\nThis code will expire in 5 minutes. Do not share it with anyone.");
        
        System.out.println("[EmailService] Sending OTP to " + to + ": " + otp);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log but don't crash if SMTP is unconfigured for local dev
            System.err.println("Failed to send real email (SMTP likely unconfigured). Fallback to console: OTP=" + otp);
        }
    }
}
