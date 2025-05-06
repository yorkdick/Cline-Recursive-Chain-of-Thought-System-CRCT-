package com.rayfay.gira.dto.response;

import com.rayfay.gira.entity.UserRole;
import com.rayfay.gira.entity.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
}