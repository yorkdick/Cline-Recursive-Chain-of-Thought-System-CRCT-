package com.rayfay.gira.controller;

import com.rayfay.gira.dto.request.UpdateBoardRequest;
import com.rayfay.gira.dto.response.BoardResponse;
import com.rayfay.gira.dto.response.TaskResponse;
import com.rayfay.gira.entity.BoardStatus;
import com.rayfay.gira.service.interfaces.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request) {
        return ResponseEntity.ok(boardService.updateBoard(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    @GetMapping("/active")
    public ResponseEntity<BoardResponse> getActiveBoard() {
        return ResponseEntity.ok(boardService.getActiveBoard());
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getBoardTasks(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoardTasks(id));
    }

    @GetMapping
    public ResponseEntity<Page<BoardResponse>> getAllBoards(
            @RequestParam(required = false) BoardStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(boardService.getAllBoards(status, pageable));
    }
}