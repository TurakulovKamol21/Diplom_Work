package com.ai.financial.repository;

import com.ai.financial.entity.GDPData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GDPDataRepository extends JpaRepository<GDPData, Long> {
    
    List<GDPData> findAllByOrderByYearAscQuarterAsc();
}
