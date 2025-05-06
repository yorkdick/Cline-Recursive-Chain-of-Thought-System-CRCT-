package com.rayfay.gira.repository;

import com.rayfay.gira.entity.Sprint;
import com.rayfay.gira.entity.SprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    Page<Sprint> findByStatus(SprintStatus status, Pageable pageable);

    List<Sprint> findByStatusAndEndDateBefore(SprintStatus status, LocalDate date);

    boolean existsByStatus(SprintStatus status);

    boolean existsByName(String name);
}