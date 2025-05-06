package com.rayfay.gira.dto.response;

import com.rayfay.gira.entity.BoardStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoardResponse {
    private Long id;
    private String name;
    private String description;
    private BoardStatus status;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
}