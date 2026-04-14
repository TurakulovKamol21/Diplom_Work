package com.ai.financial.repository;

import com.ai.financial.entity.InflationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InflationDataRepository extends JpaRepository<InflationData, Long> {
    
    List<InflationData> findAllByOrderByPeriodAsc();
}
