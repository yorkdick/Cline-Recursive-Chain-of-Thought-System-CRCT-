package com.rayfay.gira.controller;

import com.rayfay.gira.dto.request.CreateSprintRequest;
import com.rayfay.gira.dto.request.UpdateSprintRequest;
import com.rayfay.gira.dto.response.SprintResponse;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.SprintStatus;
import com.rayfay.gira.service.interfaces.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse createSprint(@Valid @RequestBody CreateSprintRequest request) {
        return sprintService.createSprint(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse updateSprint(@PathVariable Long id, @Valid @RequestBody UpdateSprintRequest request) {
        return sprintService.updateSprint(id, request);
    }

    @GetMapping("/{id}")
    public SprintResponse getSprint(@PathVariable Long id) {
        return sprintService.getSprintById(id);
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse startSprint(@PathVariable Long id) {
        return sprintService.startSprint(id);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse completeSprint(@PathVariable Long id) {
        return sprintService.completeSprint(id);
    }

    @GetMapping
    public Page<SprintResponse> getAllSprints(
            @RequestParam(required = false) SprintStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return sprintService.getAllSprints(status, pageable);
    }

    @GetMapping("/{id}/tasks")
    public List<TaskResponse> getSprintTasks(@PathVariable Long id) {
        return sprintService.getSprintTasks(id);
    }
}