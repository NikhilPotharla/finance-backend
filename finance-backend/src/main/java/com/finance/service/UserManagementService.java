package com.finance.service;

import com.finance.dto.UpdateUserRoleRequest;
import com.finance.dto.UpdateUserStatusRequest;
import com.finance.dto.UserResponse;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.User;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    // ─────────────── List all users ───────────────

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────── Get single user ───────────────

    public UserResponse getUserById(Long id) {
        return toResponse(findUser(id));
    }

    // ─────────────── Update active status ───────────────

    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        User user = findUser(id);
        user.setActive(request.getActive());
        return toResponse(userRepository.save(user));
    }

    // ─────────────── Update role ───────────────

    public UserResponse updateRole(Long id, UpdateUserRoleRequest request) {
        User user = findUser(id);
        user.setRole(request.getRole());
        return toResponse(userRepository.save(user));
    }

    // ─────────────── Helper ───────────────

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }
}
