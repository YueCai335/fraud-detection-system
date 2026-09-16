package com.yuecai.fraud.prediction;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Page<Prediction> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    List<Prediction> findByBatchIdAndUserUsernameOrderByCsvRowAsc(String batchId, String username);

    long countByBatchIdAndFraudTrue(String batchId);

    boolean existsByUserUsername(String username);
}
