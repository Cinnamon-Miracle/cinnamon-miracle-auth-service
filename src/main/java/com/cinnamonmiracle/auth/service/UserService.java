package com.cinnamonmiracle.auth.service;

import com.cinnamonmiracle.auth.entity.User;
import com.cinnamonmiracle.auth.repository.UserRepository;
import com.cinnamonmiracle.common.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of controllers/userController.js */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> getUsers(int page, int size) {
        try {
            if (page < 1) {
                page = 1;
            }
            long totalUsers = userRepository.count();
            int totalPages = (int) Math.ceil((double) totalUsers / size);

            // Sort by id ascending to match MongoDB's natural (_id insertion) order.
            List<User> users = userRepository.findAll(PageRequest.of(page - 1, size,
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id")))
                    .getContent();

            Map<String, Object> pagination = new LinkedHashMap<>();
            pagination.put("currentPage", page);
            pagination.put("totalPages", totalPages);
            pagination.put("totalUsers", totalUsers);
            pagination.put("size", size);
            pagination.put("hasNextPage", page < totalPages);
            pagination.put("hasPrevPage", page > 1);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("users", users);
            response.put("pagination", pagination);
            return response;
        } catch (Exception e) {
            throw new ApiException(500, Map.of("message", "Server error"));
        }
    }

    public User createUser(User user) {
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            throw new ApiException(400, Map.of("message", "Invalid data"));
        }
    }

    public User getUserById(String id) {
        try {
            return userRepository.findById(id)
                    .orElseThrow(() -> new ApiException(404, Map.of("message", "User not found")));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, Map.of("message", "Server error"));
        }
    }

    public User updateUser(String id, Map<String, Object> body) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                throw new ApiException(404, Map.of("message", "User not found"));
            }

            body.remove("password"); // Prevent password update here

            applyIfPresent(body, "firstName", v -> user.setFirstName((String) v));
            applyIfPresent(body, "lastName", v -> user.setLastName((String) v));
            applyIfPresent(body, "email", v -> user.setEmail(v == null ? null : ((String) v).toLowerCase()));
            applyIfPresent(body, "role", v -> user.setRole((String) v));
            applyIfPresent(body, "phone", v -> user.setPhone((String) v));
            applyIfPresent(body, "address", v -> user.setAddress((String) v));
            applyIfPresent(body, "dob", v -> user.setDob((String) v));
            applyIfPresent(body, "mobile", v -> user.setMobile((String) v));
            applyIfPresent(body, "profilePicture", v -> user.setProfilePicture((String) v));

            return userRepository.save(user);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, Map.of("message", e.getMessage() == null ? "Invalid data" : e.getMessage()));
        }
    }

    public Map<String, Object> deleteUser(String id, String currentUserId) {
        try {
            if (!isValidId(id)) {
                throw new ApiException(400, Map.of("message", "Invalid user ID format"));
            }

            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                throw new ApiException(404, Map.of("message", "User not found"));
            }

            if (user.getId().equals(currentUserId)) {
                throw new ApiException(400, Map.of("message", "Cannot delete your own account"));
            }

            if ("root".equals(user.getRole())) {
                throw new ApiException(403, Map.of("message", "Cannot delete root user"));
            }

            userRepository.deleteById(id);

            Map<String, Object> deletedUser = new LinkedHashMap<>();
            deletedUser.put("id", user.getId());
            deletedUser.put("email", user.getEmail());
            deletedUser.put("firstName", user.getFirstName());
            deletedUser.put("lastName", user.getLastName());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "User deleted successfully");
            response.put("deletedUser", deletedUser);
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, Map.of("message", "Server error", "error",
                    e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    private interface Setter {
        void set(Object value);
    }

    private void applyIfPresent(Map<String, Object> body, String key, Setter setter) {
        if (body.containsKey(key)) {
            setter.set(body.get(key));
        }
    }

    private boolean isValidId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        // Accept both migrated Mongo ObjectIds (24 hex) and generated UUIDs.
        if (id.matches("[0-9a-fA-F]{24}")) {
            return true;
        }
        try {
            java.util.UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
