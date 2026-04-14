package com.ai.financial.repository;

import com.ai.financial.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByForecastRunIdOrderByTargetDateAsc(Long forecastRunId);
}
