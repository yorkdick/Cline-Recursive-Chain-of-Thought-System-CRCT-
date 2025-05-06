package com.rayfay.gira.mapper;

import com.rayfay.gira.dto.request.CreateUserRequest;
import com.rayfay.gira.dto.response.UserResponse;
import com.rayfay.gira.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);
}