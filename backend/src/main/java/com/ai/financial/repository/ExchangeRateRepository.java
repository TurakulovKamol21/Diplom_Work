package com.ai.financial.repository;

import com.ai.financial.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    List<ExchangeRate> findByCurrencyCodeOrderByDateAsc(String currencyCode);
    
    Optional<ExchangeRate> findTopByCurrencyCodeOrderByDateDesc(String currencyCode);
}
