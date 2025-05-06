package com.rayfay.gira.repository;

import com.rayfay.gira.entity.Board;
import com.rayfay.gira.entity.BoardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByName(String name);

    Optional<Board> findByStatus(BoardStatus status);

    Page<Board> findByStatus(BoardStatus status, Pageable pageable);
}