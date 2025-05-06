package com.rayfay.gira.dto.request;

import com.rayfay.gira.entity.UserStatus;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private UserStatus status;
}