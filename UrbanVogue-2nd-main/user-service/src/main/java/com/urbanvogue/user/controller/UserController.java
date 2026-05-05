package com.urbanvogue.user.controller;

import com.urbanvogue.user.dto.UserProfileDashboard;
import com.urbanvogue.user.model.UserProfile;
import com.urbanvogue.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Create or Update Profile
    @PostMapping
    public UserProfile saveProfile(@RequestBody UserProfile profile) {
        return userService.createOrUpdateProfile(profile);
    }

    // ── NEW: Amazon-style "My Account" endpoint (scoped to logged-in user) ──
    @GetMapping("/me")
    public UserProfile getMyProfile(@RequestHeader("X-Logged-In-User") String username) {
        return userService.getProfile(username);
    }

    // ── NEW: Amazon-style "My Dashboard" endpoint (scoped to logged-in user) ──
    @GetMapping("/me/dashboard")
    public UserProfileDashboard getMyDashboard(@RequestHeader("X-Logged-In-User") String username) {
        return userService.getDashboard(username);
    }

    // ── NEW: Check if profile exists (for auth-service login verification) ──
    @GetMapping("/exists/{username}")
    public boolean profileExists(@PathVariable String username) {
        return userService.profileExists(username);
    }

    // Get Basic Profile Details (for internal service-to-service calls)
    @GetMapping("/{username}")
    public UserProfile getProfile(@PathVariable String username) {
        return userService.getProfile(username);
    }

    // Get Profile Dashboard (Includes Order History) (for internal service-to-service calls)
    @GetMapping("/{username}/dashboard")
    public UserProfileDashboard getDashboard(@PathVariable String username) {
        return userService.getDashboard(username);
    }

    // Delete Account (Cascades to Auth Service)
    @DeleteMapping("/{username}")
    public String deleteAccount(@PathVariable String username) {
        userService.deleteAccount(username);
        return "Account successfully deleted for user: " + username;
    }
}
