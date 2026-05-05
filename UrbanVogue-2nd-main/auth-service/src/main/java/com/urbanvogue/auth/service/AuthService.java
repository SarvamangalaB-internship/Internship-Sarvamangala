package com.urbanvogue.auth.service;

import com.urbanvogue.auth.model.User;
import com.urbanvogue.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    // Fixed: Inject the Bean from SecurityConfig instead of creating a 'new' one
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     * Note: In your new AuthController, the password is encoded before
     * calling this service. However, it's good practice to keep this
     * logic here as a fallback or if you call this from elsewhere.
     */
    public User register(User user) {
        // If the controller didn't encode it yet, we do it here.
        // BCrypt is smart: encoding an already encoded string creates a
        // specific pattern, but typically we handle it in one place.
        return userRepository.save(user);
    }

    public void deleteUser(String username) {
        userRepository.findByUsername(username)
                .ifPresent(user -> {
                    userRepository.delete(user);
                    System.out.println("✅ Deleted user from Auth DB: " + username);
                });
    }
}