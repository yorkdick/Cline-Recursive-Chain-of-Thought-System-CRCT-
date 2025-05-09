package com.rayfay.gira.dto.request;

import com.rayfay.gira.entity.UserStatus;
import com.rayfay.gira.entity.UserRole;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String email;
    private String fullName;
    private UserStatus status;
    private UserRole role;
}
