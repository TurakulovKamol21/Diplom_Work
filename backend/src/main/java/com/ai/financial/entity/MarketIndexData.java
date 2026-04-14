package com.ai.financial.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "market_index_data")
public class MarketIndexData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double uciIndex; // Uzbekistan Composite Index (TSE)

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getUciIndex() { return uciIndex; }
    public void setUciIndex(Double uciIndex) { this.uciIndex = uciIndex; }
}
