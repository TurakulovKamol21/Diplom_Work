package com.ai.financial.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inflation_data")
public class InflationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String period; // e.g., "2026-03"

    @Column(name = "monthly_rate")
    private Double monthlyRate; // Monthly percentage change

    @Column(name = "annual_rate")
    private Double annualRate; // Year-over-Year percentage change

    @Column(length = 50)
    private String category; // e.g., "General", "Food", "Non-Food"

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Double getMonthlyRate() { return monthlyRate; }
    public void setMonthlyRate(Double monthlyRate) { this.monthlyRate = monthlyRate; }

    public Double getAnnualRate() { return annualRate; }
    public void setAnnualRate(Double annualRate) { this.annualRate = annualRate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
