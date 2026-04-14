package com.ai.financial.repository;

import com.ai.financial.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    RiskAssessment findTopByOrderByAssessmentDateDesc();
}
