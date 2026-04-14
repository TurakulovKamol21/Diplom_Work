package com.ai.financial.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gdp_data")
public class GDPData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year; // e.g., 2025

    @Column(nullable = false)
    private Integer quarter; // 1, 2, 3, 4

    @Column(name = "volume_billion_uzs")
    private Double volumeBillionUzs; // Total GDP volume

    @Column(name = "growth_rate")
    private Double growthRate; // Percentage growth compared to previous year/quarter

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getQuarter() { return quarter; }
    public void setQuarter(Integer quarter) { this.quarter = quarter; }

    public Double getVolumeBillionUzs() { return volumeBillionUzs; }
    public void setVolumeBillionUzs(Double volumeBillionUzs) { this.volumeBillionUzs = volumeBillionUzs; }

    public Double getGrowthRate() { return growthRate; }
    public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }
}
