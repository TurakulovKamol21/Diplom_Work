package com.ai.financial.repository;

import com.ai.financial.entity.PolicyRateData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRateDataRepository extends JpaRepository<PolicyRateData, Long> {
    List<PolicyRateData> findAllByOrderByDateAsc();
}
