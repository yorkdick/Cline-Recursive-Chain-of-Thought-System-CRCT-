package com.rayfay.gira.service.interfaces;

import com.rayfay.gira.dto.request.CreateUserRequest;
import com.rayfay.gira.dto.request.UpdateUserRequest;
import com.rayfay.gira.dto.response.UserResponse;
import com.rayfay.gira.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User findByUsername(String username);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void updatePassword(Long id, String oldPassword, String newPassword);

    UserResponse getUserById(Long id);

    Page<UserResponse> getAllUsers(Pageable pageable);

    void deleteUser(Long id);
}