package com.rayfay.gira.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sprints", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sprint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "sprint")
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToOne(mappedBy = "sprint", fetch = FetchType.LAZY)
    private Board board;
}