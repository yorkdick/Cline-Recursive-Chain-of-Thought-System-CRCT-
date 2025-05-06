package com.rayfay.gira.mapper;

import com.rayfay.gira.dto.response.SprintResponse;
import com.rayfay.gira.entity.Sprint;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { UserMapper.class,
        BoardMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SprintMapper {
    @Mapping(target = "board", source = "board")
    SprintResponse toResponse(Sprint sprint);
}