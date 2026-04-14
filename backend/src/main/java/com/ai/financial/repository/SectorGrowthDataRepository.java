package com.ai.financial.repository;

import com.ai.financial.entity.SectorGrowthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorGrowthDataRepository extends JpaRepository<SectorGrowthData, Long> {
    List<SectorGrowthData> findAllByOrderByYearAscQuarterAsc();
}
