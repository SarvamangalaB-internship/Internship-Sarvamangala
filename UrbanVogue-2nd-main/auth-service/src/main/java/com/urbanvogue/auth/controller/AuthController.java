package com.urbanvogue.auth.controller;

import com.urbanvogue.auth.dto.RegistrationRequest;
import com.urbanvogue.auth.model.User;
import com.urbanvogue.auth.repository.UserRepository;
import com.urbanvogue.auth.service.AuthService;
import com.urbanvogue.auth.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Fixed: Injecting the password encoder bean from SecurityConfig
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    // ── REGISTER (Amazon-style: One form creates both auth + profile) ──
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegistrationRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Create auth credentials in users table
            User user = new User();
            user.setUsername(request.getUsername());
            // Encode password using the injected bean
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole() != null ? request.getRole() : "ROLE_USER");

            User savedUser = authService.register(user);

            // 2. Auto-create user profile in user-service
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("username", request.getUsername());
            profileData.put("fullName", request.getFullName());
            profileData.put("age", request.getAge());
            profileData.put("sex", request.getSex());
            profileData.put("address", request.getAddress());
            profileData.put("email", request.getEmail());

            try {
                String profileUrl = userServiceUrl + "/api/users";
                restTemplate.postForObject(profileUrl, profileData, Object.class);
                System.out.println("✅ User profile created for: " + request.getUsername());
            } catch (Exception e) {
                System.err.println("⚠️ Could not create user profile: " + e.getMessage());
            }

            // ── Notify: Send welcome email ──
            try {
                String notifyUrl = notificationServiceUrl +
                        "/api/notifications/welcome?username=" +
                        request.getUsername();
                restTemplate.postForObject(notifyUrl, null, Object.class);
            } catch (Exception e) {
                System.err.println("⚠️ Could not send welcome notification: " + e.getMessage());
            }

            response.put("success", true);
            response.put("message", "Registration successful! Welcome to Urban Vogue.");
            response.put("username", request.getUsername());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
        }

        return response;
    }

    // ── LOGIN — Returns token + user info ──
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Find user by username
            User user = userRepository
                    .findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. LOGGING for debugging (Check console if login fails)
            System.out.println("Login attempt for: " + loginRequest.getUsername());

            // 3. SECURE MATCHING
            // matches(Raw_Password, Encoded_Password_from_DB)
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                System.err.println("❌ Password mismatch for user: " + loginRequest.getUsername());
                throw new RuntimeException("Invalid Credentials");
            }

            // 4. Generate JWT
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

            // ── Notify: Send login notification (Async-like) ──
            try {
                String notifyUrl = notificationServiceUrl + "/api/notifications/send";
                Map<String, Object> notificationBody = new HashMap<>();
                notificationBody.put("recipientUsername", user.getUsername());
                notificationBody.put("type", "LOGIN");
                notificationBody.put("channel", "EMAIL");
                notificationBody.put("message", "Hello " + user.getUsername() + "! Successful login to UrbanVogue.");

                restTemplate.postForObject(notifyUrl, notificationBody, Object.class);
            } catch (Exception e) {
                System.err.println("⚠️ Notification failed: " + e.getMessage());
            }

            response.put("success", true);
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("userId", user.getId());
            response.put("message", "Login successful!");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    // ── VALIDATE — Hub for Gateway and other services ──
    @PostMapping("/validate")
    public Map<String, Object> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("message", "No token provided");
            return response;
        }

        String token = authHeader.substring(7);

        if (jwtUtils.validateToken(token)) {
            response.put("valid", true);
            response.put("username", jwtUtils.getUsernameFromToken(token));
            response.put("role", jwtUtils.getRoleFromToken(token));
            response.put("expired", false);
        } else {
            response.put("valid", false);
            response.put("expired", true);
            response.put("message", "Token is invalid.");
        }

        return response;
    }

    // ── DELETE USER CREDENTIALS ──
    @DeleteMapping("/{username}")
    public String deleteUser(@PathVariable String username) {
        authService.deleteUser(username);
        return "Auth credentials deleted for " + username;
    }
}