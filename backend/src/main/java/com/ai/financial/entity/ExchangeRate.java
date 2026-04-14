package com.ai.financial.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currencyCode; // e.g., "USD"

    @Column(nullable = false)
    private Double rate; // Value against UZS

    @Column(name = "rate_diff")
    private Double diff; // Difference from previous day

    @Column(nullable = false)
    private LocalDate date; // Date of the rate

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Double getDiff() { return diff; }
    public void setDiff(Double diff) { this.diff = diff; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
