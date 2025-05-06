package com.rayfay.gira.dto.request;

import com.rayfay.gira.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {
    @NotNull(message = "状态不能为空")
    private TaskStatus status;
}