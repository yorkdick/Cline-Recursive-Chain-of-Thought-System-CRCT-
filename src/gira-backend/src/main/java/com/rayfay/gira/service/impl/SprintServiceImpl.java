package com.rayfay.gira.service.impl;

import com.rayfay.gira.dto.request.CreateSprintRequest;
import com.rayfay.gira.dto.request.UpdateSprintRequest;
import com.rayfay.gira.dto.response.SprintResponse;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.Board;
import com.rayfay.gira.entity.BoardStatus;
import com.rayfay.gira.entity.Sprint;
import com.rayfay.gira.entity.SprintStatus;
import com.rayfay.gira.entity.Task;
import com.rayfay.gira.entity.TaskStatus;
import com.rayfay.gira.entity.User;
import com.rayfay.gira.exception.ResourceNotFoundException;
import com.rayfay.gira.mapper.SprintMapper;
import com.rayfay.gira.mapper.TaskMapper;
import com.rayfay.gira.repository.SprintRepository;
import com.rayfay.gira.repository.TaskRepository;
import com.rayfay.gira.repository.UserRepository;
import com.rayfay.gira.repository.BoardRepository;
import com.rayfay.gira.service.interfaces.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request) {
        // 检查结束日期是否在开始日期之后
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("结束日期必须在开始日期之后");
        }

        // 检查Sprint持续时间是否超过4周
        long duration = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (duration > 28) {
            throw new IllegalArgumentException("Sprint持续时间不能超过4周");
        }

        // 检查是否存在活动中的Sprint
        if (sprintRepository.existsByStatus(SprintStatus.ACTIVE)) {
            throw new IllegalStateException("已存在活动中的Sprint");
        }

        // 检查Sprint名称是否已存在
        if (sprintRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Sprint名称已存在");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Sprint sprint = Sprint.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SprintStatus.PLANNING)
                .createdBy(currentUser)
                .build();

        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    @Override
    public SprintResponse getSprintById(Long id) {
        return sprintMapper.toResponse(getSprintOrThrow(id));
    }

    @Override
    public Page<SprintResponse> getAllSprints(SprintStatus status, Pageable pageable) {
        if (status != null) {
            return sprintRepository.findByStatus(status, pageable)
                    .map(sprintMapper::toResponse);
        }
        return sprintRepository.findAll(pageable)
                .map(sprintMapper::toResponse);
    }

    @Override
    @Transactional
    public SprintResponse startSprint(Long id) {
        Sprint sprint = getSprintOrThrow(id);

        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new IllegalStateException("只能启动计划中的Sprint");
        }

        // 检查是否存在活动中的Sprint
        if (sprintRepository.existsByStatus(SprintStatus.ACTIVE)) {
            throw new IllegalStateException("已存在活动中的Sprint");
        }

        // 检查是否有任务
        int taskCount = taskRepository.countBySprintId(sprint.getId());
        if (taskCount == 0) {
            throw new IllegalStateException("Sprint中没有任务，无法启动");
        }

        // 创建看板
        Board board = Board.builder()
                .name(sprint.getName() + "看板")
                .description("Sprint: " + sprint.getName())
                .status(BoardStatus.ACTIVE)
                .createdBy(sprint.getCreatedBy())
                .sprint(sprint)
                .build();

        // 保存看板
        board = boardRepository.save(board);

        // 更新Sprint状态和关联
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setBoard(board);
        sprint = sprintRepository.save(sprint);

        return sprintMapper.toResponse(sprint);
    }

    @Override
    @Transactional
    public SprintResponse completeSprint(Long id) {
        Sprint sprint = getSprintOrThrow(id);

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IllegalStateException("只能完成活动中的Sprint");
        }

        // 自动完成所有未完成的任务
        List<Task> unfinishedTasks = taskRepository.findBySprintIdAndStatusIn(
                sprint.getId(),
                List.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO));

        for (Task task : unfinishedTasks) {
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
        }

        // 归档关联的看板
        Board board = sprint.getBoard();
        if (board != null) {
            board.setStatus(BoardStatus.ARCHIVED);
            boardRepository.save(board);
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    @Override
    public List<TaskResponse> getSprintTasks(Long id) {
        Sprint sprint = getSprintOrThrow(id);
        return taskRepository.findBySprintId(sprint.getId())
                .stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SprintResponse updateSprint(Long id, UpdateSprintRequest request) {
        Sprint sprint = getSprintOrThrow(id);

        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new IllegalStateException("只能修改计划中的Sprint");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("结束日期必须在开始日期之后");
        }

        // 检查Sprint持续时间是否超过4周
        long duration = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (duration > 28) {
            throw new IllegalArgumentException("Sprint持续时间不能超过4周");
        }

        // 检查Sprint名称是否已存在（如果名称发生变化）
        if (!request.getName().equals(sprint.getName()) && sprintRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Sprint名称已存在");
        }

        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());

        return sprintMapper.toResponse(sprintRepository.save(sprint));
    }

    private Sprint getSprintOrThrow(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint不存在"));
    }
}