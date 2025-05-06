package com.rayfay.gira.dto.response;

import com.rayfay.gira.entity.SprintStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SprintResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatus status;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
    private BoardResponse board;
}