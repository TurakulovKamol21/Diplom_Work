package com.ai.financial.repository;

import com.ai.financial.entity.MarketIndexData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketIndexDataRepository extends JpaRepository<MarketIndexData, Long> {
    List<MarketIndexData> findAllByOrderByDateAsc();
}
