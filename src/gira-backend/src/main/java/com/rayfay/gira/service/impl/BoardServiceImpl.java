package com.rayfay.gira.service.impl;

import com.rayfay.gira.dto.request.UpdateBoardRequest;
import com.rayfay.gira.dto.response.BoardResponse;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.Board;
import com.rayfay.gira.entity.BoardStatus;
import com.rayfay.gira.entity.Sprint;
import com.rayfay.gira.exception.ResourceNotFoundException;
import com.rayfay.gira.mapper.BoardMapper;
import com.rayfay.gira.repository.BoardRepository;
import com.rayfay.gira.service.interfaces.BoardService;
import com.rayfay.gira.service.interfaces.SprintService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final SprintService sprintService;
    private final BoardMapper boardMapper;

    @Override
    @Transactional
    public BoardResponse updateBoard(Long id, UpdateBoardRequest request) {
        Board board = getBoardOrThrow(id);

        if (board.getStatus() == BoardStatus.ARCHIVED) {
            throw new IllegalStateException("已归档的看板不能修改");
        }

        // 如果修改了名称，检查新名称是否已存在
        if (!board.getName().equals(request.getName()) &&
                boardRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("看板名称已存在");
        }

        board.setName(request.getName());
        board.setDescription(request.getDescription());

        return boardMapper.toResponse(boardRepository.save(board));
    }

    @Override
    public BoardResponse getBoardById(Long id) {
        return boardMapper.toResponse(getBoardOrThrow(id));
    }

    @Override
    public BoardResponse getActiveBoard() {
        return boardRepository.findByStatus(BoardStatus.ACTIVE)
                .map(boardMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("没有活动中的看板"));
    }

    @Override
    public Page<BoardResponse> getAllBoards(BoardStatus status, Pageable pageable) {
        if (status != null) {
            return boardRepository.findByStatus(status, pageable)
                    .map(boardMapper::toResponse);
        }
        return boardRepository.findAll(pageable)
                .map(boardMapper::toResponse);
    }

    private Board getBoardOrThrow(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("看板不存在"));
    }

    @Override
    public List<TaskResponse> getBoardTasks(Long id) {
        Board board = getBoardOrThrow(id);
        Sprint activeSprint = board.getSprint();
        return sprintService.getSprintTasks(activeSprint.getId());
    }
}