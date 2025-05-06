package com.rayfay.gira.service.interfaces;

import com.rayfay.gira.dto.request.CreateSprintRequest;
import com.rayfay.gira.dto.request.UpdateSprintRequest;
import com.rayfay.gira.dto.response.SprintResponse;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.SprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SprintService {
    SprintResponse createSprint(CreateSprintRequest request);

    SprintResponse updateSprint(Long id, UpdateSprintRequest request);

    SprintResponse getSprintById(Long id);

    Page<SprintResponse> getAllSprints(SprintStatus status, Pageable pageable);

    SprintResponse startSprint(Long id);

    SprintResponse completeSprint(Long id);

    List<TaskResponse> getSprintTasks(Long id);
}