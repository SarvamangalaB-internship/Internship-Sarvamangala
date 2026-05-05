package com.urbanvogue.user.service;

import com.urbanvogue.user.dto.UserProfileDashboard;
import com.urbanvogue.user.model.UserProfile;
import com.urbanvogue.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public UserProfile createOrUpdateProfile(UserProfile profile) {
        UserProfile existing = userRepository.findByUsername(profile.getUsername()).orElse(null);
        if (existing != null) {
            existing.setFullName(profile.getFullName());
            existing.setAge(profile.getAge());
            existing.setSex(profile.getSex());
            existing.setAddress(profile.getAddress());
            existing.setEmail(profile.getEmail());
            return userRepository.save(existing);
        }
        return userRepository.save(profile);
    }

    public UserProfile getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for: " + username));
    }

    public UserProfileDashboard getDashboard(String username) {
        UserProfile profile = getProfile(username);
        
        // Fetch Order History from order-service
        List<Object> orders;
        try {
            String url = orderServiceUrl + "/api/orders/customer/" + username;
            Object[] orderArray = restTemplate.getForObject(url, Object[].class);
            orders = orderArray != null ? Arrays.asList(orderArray) : List.of();
        } catch (Exception e) {
            System.err.println("Could not fetch orders for " + username + ": " + e.getMessage());
            orders = List.of(); // Return empty list if order-service is down
        }

        UserProfileDashboard dashboard = new UserProfileDashboard();
        dashboard.setProfile(profile);
        dashboard.setOrderHistory(orders);
        return dashboard;
    }

    public void deleteAccount(String username) {
        UserProfile profile = getProfile(username);
        
        // 1. Delete profile from user-service DB
        userRepository.delete(profile);
        
        // 2. Instruct auth-service to delete login credentials
        try {
            String url = authServiceUrl + "/api/auth/" + username;
            restTemplate.delete(url);
            System.out.println("✅ Successfully deleted auth credentials for: " + username);
        } catch (Exception e) {
            System.err.println("⚠️ Could not delete auth credentials for " + username + ": " + e.getMessage());
        }
    }

    // ── NEW: Check if profile exists (for auth-service login verification) ──
    public boolean profileExists(String username) {
        try {
            return userRepository.findByUsername(username).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
