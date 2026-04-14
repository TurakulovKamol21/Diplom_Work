package com.ai.financial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User generatedBy;

    private LocalDateTime generatedAt;

    private String reportType; // PDF, JSON

    @Column(length = 5000)
    private String reportData; // JSON or link to file
}
