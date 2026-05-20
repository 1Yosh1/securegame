package com.unime.securegame.controller;

import com.unime.securegame.model.UserEntity;
import com.unime.securegame.repository.UserRepository;
import com.unime.securegame.service.EmailService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final Map<String, String> otps = new java.util.concurrent.ConcurrentHashMap<>();

    public AuthController(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String email, 
                                      @RequestParam String password,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) String classroomCode) {
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"Email already exists\"}");
        }
        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setPassword(hashPassword(password));
        newUser.setName(name);
        newUser.setRole(role != null ? role : "PENDING");
        newUser.setClassroomCode(classroomCode);
        newUser.setLevel(1);
        newUser.setXp(0);
        userRepository.save(newUser);
        return ResponseEntity.ok("{\"status\":\"success\"}");
    }

    @PostMapping("/update-role")
    public ResponseEntity<?> updateRole(@RequestParam String email, @RequestParam String role) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setRole(role.trim().toUpperCase());
            userRepository.save(user);
            return ResponseEntity.ok("{\"status\":\"success\", \"role\":\"" + user.getRole() + "\"}");
        }
        return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"User not found\"}");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"Invalid credentials\"}");
        }
        
        UserEntity user = userOpt.get();
        if (!user.getPassword().equals(hashPassword(password))) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"Invalid credentials\"}");
        }

        // Generate and send OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        otps.put(email, otp);
        
        emailService.sendOtpEmail(email, otp);
        return ResponseEntity.ok("{\"status\":\"otp_sent\", \"role\":\"" + user.getRole() + "\", \"otp\":\"" + otp + "\"}");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        String correctOtp = otps.remove(email.trim()); // Remove immediately to ensure single-use (prevents brute-force)
        boolean isValid = otp != null && otp.equals(correctOtp);
        if (isValid) {
            Optional<UserEntity> userOpt = userRepository.findByEmail(email.trim());
            if(userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                return ResponseEntity.ok("{\"status\":\"success\", \"role\":\"" + user.getRole() + "\", \"xp\":" + user.getXp() + ", \"level\":" + user.getLevel() + "}");
            }
        }
        return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"Invalid OTP\"}");
    }

    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestParam String email, @RequestParam int xp, @RequestParam int level) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setXp(xp);
            user.setLevel(level);
            userRepository.save(user);
            return ResponseEntity.ok("{\"status\":\"success\"}");
        }
        return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"User not found\"}");
    }

    private final Map<String, String> recoveryOtps = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"No account registered with this email\"}");
        }

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        recoveryOtps.put(email.trim(), otp);

        emailService.sendOtpEmail(email.trim(), otp);
        return ResponseEntity.ok("{\"status\":\"success\", \"message\":\"Recovery OTP sent to email\", \"otp\":\"" + otp + "\"}");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword) {
        String correctOtp = recoveryOtps.remove(email.trim());
        if (correctOtp == null || !correctOtp.equals(otp)) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"Invalid or expired OTP\"}");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"status\":\"error\", \"message\":\"User not found\"}");
        }

        UserEntity user = userOpt.get();
        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("{\"status\":\"success\", \"message\":\"Password reset successful\"}");
    }

    @GetMapping("/classroom/{code}")
    public ResponseEntity<?> getClassroomStudents(@PathVariable String code) {
        List<UserEntity> students = userRepository.findByClassroomCode(code);
        return ResponseEntity.ok(students);
    }
}
