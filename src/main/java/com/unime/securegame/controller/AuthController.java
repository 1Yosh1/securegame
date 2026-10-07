package com.unime.securegame.controller;

import com.unime.securegame.model.ClassroomEntity;
import com.unime.securegame.model.UserEntity;
import com.unime.securegame.repository.ClassroomRepository;
import com.unime.securegame.repository.UserRepository;
import com.unime.securegame.service.EmailService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final int OTP_TTL_SECONDS = 300;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final SecurityContextRepository securityContextRepository;
    private final Map<String, OtpChallenge> otps = new ConcurrentHashMap<>();
    // email -> device token mapping (in production, use a DB table with expiry)
    private final Map<String, DeviceToken> deviceTokens = new ConcurrentHashMap<>();

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOtp() {
        return String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.length() <= 254
                && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private OtpChallenge storeOtp(Map<String, OtpChallenge> store, String email, String code) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        OtpChallenge challenge = new OtpChallenge(salt, otpDigest(code, salt), Instant.now().plusSeconds(OTP_TTL_SECONDS));
        store.put(email, challenge);
        return challenge;
    }

    private byte[] otpDigest(String code, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean consumeOtp(Map<String, OtpChallenge> store, String email, String submitted) {
        AtomicBoolean valid = new AtomicBoolean(false);
        store.compute(email, (key, challenge) -> {
            if (challenge == null || challenge.expiresAt().isBefore(Instant.now())) return null;
            challenge.attempts++;
            if (MessageDigest.isEqual(challenge.codeDigest(), otpDigest(submitted, challenge.salt()))) {
                valid.set(true);
                return null;
            }
            return challenge.attempts >= OTP_MAX_ATTEMPTS ? null : challenge;
        });
        return valid.get();
    }

    private record DeviceToken(String value, Instant expiresAt) {}

    private boolean isValidDeviceToken(String email, String submittedToken) {
        DeviceToken issued = deviceTokens.get(email);
        if (issued == null) return false;
        if (issued.expiresAt().isBefore(Instant.now())) {
            deviceTokens.remove(email, issued);
            return false;
        }
        return MessageDigest.isEqual(issued.value().getBytes(StandardCharsets.UTF_8),
                submittedToken == null ? new byte[0] : submittedToken.getBytes(StandardCharsets.UTF_8));
    }

    private static final class OtpChallenge {
        private final byte[] salt;
        private final byte[] codeDigest;
        private final Instant expiresAt;
        private int attempts;

        private OtpChallenge(byte[] salt, byte[] codeDigest, Instant expiresAt) {
            this.salt = salt;
            this.codeDigest = codeDigest;
            this.expiresAt = expiresAt;
        }

        private byte[] salt() { return salt; }
        private byte[] codeDigest() { return codeDigest; }
        private Instant expiresAt() { return expiresAt; }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(EmailService emailService, UserRepository userRepository,
                          ClassroomRepository classroomRepository,
                          SecurityContextRepository securityContextRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.securityContextRepository = Objects.requireNonNull(securityContextRepository);
    }

    public AuthController(EmailService emailService, UserRepository userRepository) {
        this(emailService, userRepository, null, new HttpSessionSecurityContextRepository());
    }

    public AuthController(EmailService emailService, UserRepository userRepository,
                          SecurityContextRepository securityContextRepository) {
        this(emailService, userRepository, null, securityContextRepository);
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = derivePassword(password, salt, PBKDF2_ITERATIONS);
        return "pbkdf2-sha256$" + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) return false;
        if (storedPassword.startsWith("pbkdf2-sha256$")) {
            try {
                String[] fields = storedPassword.split("\\$", -1);
                if (fields.length != 4) return false;
                int iterations = Integer.parseInt(fields[1]);
                if (iterations < 100_000 || iterations > 2_000_000) return false;
                byte[] salt = Base64.getDecoder().decode(fields[2]);
                byte[] expected = Base64.getDecoder().decode(fields[3]);
                if (salt.length != 16 || expected.length != 32) return false;
                return MessageDigest.isEqual(expected, derivePassword(rawPassword, salt, iterations));
            } catch (RuntimeException e) {
                return false;
            }
        }
        // Legacy SHA-256 compatibility: upgraded to PBKDF2 after a successful login.
        try {
            byte[] legacy = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return storedPassword != null && MessageDigest.isEqual(
                    Base64.getEncoder().encode(legacy), storedPassword.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private byte[] derivePassword(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String email, 
                                      @RequestParam String password,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) String classroomCode) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)
                || password == null || password.length() < 12 || password.length() > 128
                || name == null || name.isBlank() || name.length() > 120) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid registration details"));
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Email already exists"));
        }
        UserEntity newUser = new UserEntity();
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(hashPassword(password));
        newUser.setName(name.trim());
        // Instructor privileges must be provisioned by an administrator, never self-selected.
        newUser.setRole("PENDING");
        // Classroom membership is assigned through the verified classroom join flow.
        newUser.setClassroomCode(null);
        newUser.setLevel(1);
        newUser.setXp(0);
        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/update-role")
    public ResponseEntity<?> updateRole(@RequestParam String email, @RequestParam String role,
                                        @RequestParam String deviceToken) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || !isAuthenticatedAs(normalizedEmail)
                || !isValidDeviceToken(normalizedEmail, deviceToken)) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Authentication required"));
        }
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("STUDENT", "SOLO").contains(normalizedRole)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Role cannot be assigned by the player"));
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setRole(normalizedRole);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "success", "role", user.getRole()));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "User not found"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || password == null || password.length() > 128) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }

        UserEntity user = userOpt.get();
        if (!passwordMatches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }
        if (!user.getPassword().startsWith("pbkdf2-sha256$")) {
            user.setPassword(hashPassword(password));
            userRepository.save(user);
        }

        // Expiring, single-use OTP; delivery failure never returns a fake success.
        String otp = generateOtp();
        OtpChallenge challenge = storeOtp(otps, normalizedEmail, otp);
        if (!emailService.sendOtpEmail(normalizedEmail, otp)) {
            otps.remove(normalizedEmail, challenge);
            return ResponseEntity.status(503).body(Map.of("status", "error", "message", "Email delivery is unavailable"));
        }
        return ResponseEntity.ok(Map.of("status", "otp_sent", "role", user.getRole()));
    }

    @PostMapping("/login-token")
    public ResponseEntity<?> loginWithToken(@RequestParam String email,
                                            @RequestParam String password,
                                            @RequestParam String deviceToken,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || password == null || password.length() > 128) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }
        UserEntity user = userOpt.get();
        if (!passwordMatches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid credentials"));
        }
        if (!user.getPassword().startsWith("pbkdf2-sha256$")) {
            user.setPassword(hashPassword(password));
            userRepository.save(user);
        }
        if (!isValidDeviceToken(normalizedEmail, deviceToken)) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Device not recognized"));
        }
        establishSession(user, request, response);

        return ResponseEntity.ok(loginResponse(user, deviceToken));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp,
                                       HttpServletRequest request, HttpServletResponse response) {
        String normalizedEmail = normalizeEmail(email);
        boolean isValid = isValidEmail(normalizedEmail) && otp != null && otp.matches("\\d{6}")
                && consumeOtp(otps, normalizedEmail, otp);
        if (isValid) {
            Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
            if(userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                // Issue a device token for passwordless subsequent logins
                String token = generateToken();
                deviceTokens.put(normalizedEmail,
                        new DeviceToken(token, Instant.now().plusSeconds(30L * 24 * 60 * 60)));
                establishSession(user, request, response);
                return ResponseEntity.ok(loginResponse(user, token));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid OTP"));
    }

    private void establishSession(UserEntity user, HttpServletRequest request, HttpServletResponse response) {
        String role = user.getRole() == null ? "PENDING" : user.getRole().toUpperCase(Locale.ROOT);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        SecurityContextHolder.setContext(context);
        var session = request.getSession(true);
        session.setAttribute("authenticatedEmail", user.getEmail());
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }

    private boolean isAuthenticatedAs(String normalizedEmail) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && normalizedEmail.equals(authentication.getName());
    }

    private Map<String, Object> loginResponse(UserEntity user, String token) {
        return Map.of("status", "success", "role", user.getRole(), "xp", user.getXp(),
                "level", user.getLevel(), "deviceToken", token);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String email,
                                    @RequestParam(required = false) String deviceToken,
                                    HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || !isAuthenticatedAs(normalizedEmail)) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Authentication required"));
        }
        DeviceToken issued = deviceTokens.get(normalizedEmail);
        if (issued != null && deviceToken != null && isValidDeviceToken(normalizedEmail, deviceToken)) {
            deviceTokens.remove(normalizedEmail, issued);
        }
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestParam String email, @RequestParam int xp,
                                            @RequestParam int level, @RequestParam String deviceToken) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || !isAuthenticatedAs(normalizedEmail)
                || !isValidDeviceToken(normalizedEmail, deviceToken)) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Authentication required"));
        }
        if (xp < 0 || xp >= 1000 || level < 1 || level > 1000) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid progress"));
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setXp(xp);
            user.setLevel(level);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("status", "success"));
        }
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "User not found"));
    }

    private final Map<String, OtpChallenge> recoveryOtps = new ConcurrentHashMap<>();

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid email"));
        }
        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            // Use the same response as a known address to avoid account enumeration.
            return ResponseEntity.ok(Map.of("status", "success", "message", "If the account exists, a recovery code was sent"));
        }

        String otp = generateOtp();
        OtpChallenge challenge = storeOtp(recoveryOtps, normalizedEmail, otp);
        if (!emailService.sendOtpEmail(normalizedEmail, otp)) {
            recoveryOtps.remove(normalizedEmail, challenge);
        }
        // Keep the response identical whether delivery succeeded or the address exists.
        return ResponseEntity.ok(Map.of("status", "success", "message", "If the account exists, a recovery code was sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword,
                                           HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid or expired OTP"));
        }
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 128) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Password must be 12 to 128 characters"));
        }
        if (otp == null || !otp.matches("\\d{6}")
                || !consumeOtp(recoveryOtps, normalizedEmail, otp)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid or expired OTP"));
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid or expired OTP"));
        }

        UserEntity user = userOpt.get();
        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);

        deviceTokens.remove(normalizedEmail);
        var session = request.getSession(false);
        if (session != null && normalizedEmail.equals(session.getAttribute("authenticatedEmail"))) session.invalidate();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successful"));
    }

    @GetMapping("/classroom/{code}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<?> getClassroomStudents(@PathVariable String code,
                                                   org.springframework.security.core.Authentication authentication) {
        if (code == null || !code.matches("[A-Za-z0-9-]{1,32}")) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid classroom code"));
        }
        Optional<ClassroomEntity> classroom = classroomRepository.findByCode(code);
        if (classroom.isEmpty()) return ResponseEntity.notFound().build();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && !classroom.get().getTeacherEmail().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        List<Map<String, Object>> students = userRepository.findByClassroomCode(code).stream()
                .map(user -> Map.<String, Object>of("email", user.getEmail(), "name", user.getName(),
                        "role", user.getRole(), "xp", user.getXp(), "level", user.getLevel()))
                .toList();
        return ResponseEntity.ok(students);
    }
}
