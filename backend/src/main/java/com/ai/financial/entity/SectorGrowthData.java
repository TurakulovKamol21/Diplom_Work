package com.ai.financial.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sector_growth_data")
public class SectorGrowthData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer quarter;

    private Double industryGrowth;     // percentage
    private Double agricultureGrowth;  // percentage
    private Double servicesGrowth;     // percentage

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getQuarter() { return quarter; }
    public void setQuarter(Integer quarter) { this.quarter = quarter; }

    public Double getIndustryGrowth() { return industryGrowth; }
    public void setIndustryGrowth(Double industryGrowth) { this.industryGrowth = industryGrowth; }

    public Double getAgricultureGrowth() { return agricultureGrowth; }
    public void setAgricultureGrowth(Double agricultureGrowth) { this.agricultureGrowth = agricultureGrowth; }

    public Double getServicesGrowth() { return servicesGrowth; }
    public void setServicesGrowth(Double servicesGrowth) { this.servicesGrowth = servicesGrowth; }
}
