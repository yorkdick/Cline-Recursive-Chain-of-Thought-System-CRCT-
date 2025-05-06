package com.rayfay.gira.mapper;

import com.rayfay.gira.dto.response.BoardResponse;
import com.rayfay.gira.entity.Board;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { UserMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoardMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    BoardResponse toResponse(Board board);
}