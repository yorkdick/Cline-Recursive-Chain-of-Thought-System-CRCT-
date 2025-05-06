package com.rayfay.gira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBoardRequest {
    @NotBlank(message = "看板名称不能为空")
    private String name;

    private String description;
}