package com.cinnamonmiracle.auth.service;

import com.cinnamonmiracle.auth.dto.LoginRequest;
import com.cinnamonmiracle.auth.dto.RegisterRequest;
import com.cinnamonmiracle.auth.entity.User;
import com.cinnamonmiracle.auth.repository.UserRepository;
import com.cinnamonmiracle.common.exception.ApiException;
import com.cinnamonmiracle.common.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Port of controllers/authController.js */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(RegisterRequest req) {
        try {
            String email = req.email == null ? null : req.email.toLowerCase();

            Optional<User> existing = email == null ? Optional.empty() : userRepository.findByEmail(email);
            if (existing.isPresent()) {
                throw new ApiException(400, Map.of("message", "User already exists"));
            }

            User user = new User();
            user.setFirstName(req.firstName);
            user.setLastName(req.lastName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(req.password));
            user.setRole(req.role);
            user.setPhone(req.phone);
            user.setAddress(req.address);
            user.setProfilePicture(req.profilePicture);

            user = userRepository.save(user);

            String accessToken = jwtUtil.generateRegisterToken(user.getId());
            return buildAuthResponse(accessToken, user);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, Map.of("message", e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    public Map<String, Object> login(LoginRequest req) {
        try {
            String email = req.email == null ? null : req.email.toLowerCase();

            User user = (email == null ? Optional.<User>empty() : userRepository.findByEmail(email))
                    .orElseThrow(() -> new ApiException(400, Map.of("message", "Invalid credentials")));

            boolean isMatch = req.password != null && passwordEncoder.matches(req.password, user.getPassword());
            if (!isMatch) {
                throw new ApiException(400, Map.of("message", "Invalid credentials"));
            }

            String accessToken = jwtUtil.generateLoginToken(user.getId());
            return buildAuthResponse(accessToken, user);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, Map.of("message", e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    private Map<String, Object> buildAuthResponse(String accessToken, User user) {
        Map<String, Object> userView = new LinkedHashMap<>();
        userView.put("firstName", user.getFirstName());
        userView.put("lastName", user.getLastName());
        userView.put("role", user.getRole());
        userView.put("profilePicture", user.getProfilePicture());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("user", userView);
        return response;
    }
}
