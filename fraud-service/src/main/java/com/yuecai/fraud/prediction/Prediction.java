package com.yuecai.fraud.prediction;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Audit record of every scored transaction, single or batch. */
@Entity
@Table(name = "predictions")
public class Prediction {

    public enum Source { SINGLE, BATCH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;

    /** Groups the rows of one CSV upload; null for single predictions. */
    @Column(name = "batch_id", length = 36)
    private String batchId;

    /** 1-based line number in the uploaded CSV; null for single predictions. */
    @Column(name = "csv_row")
    private Integer csvRow;

    @Column(nullable = false) private int step;
    @Column(name = "type_code", nullable = false) private int typeCode;
    @Column(nullable = false) private double amount;
    @Column(name = "oldbalance_org", nullable = false) private double oldbalanceOrg;
    @Column(name = "newbalance_orig", nullable = false) private double newbalanceOrig;
    @Column(name = "oldbalance_dest", nullable = false) private double oldbalanceDest;
    @Column(name = "newbalance_dest", nullable = false) private double newbalanceDest;

    @Column(nullable = false) private boolean fraud;
    @Column(name = "prob_fraud") private Double probFraud;
    @Column(nullable = false) private double threshold;
    @Column(length = 255) private String reason1;
    @Column(length = 255) private String reason2;
    @Column(length = 255) private String reason3;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Prediction() {
        // JPA
    }

    public static Prediction single(User user, ModelFeatures f, ModelScore s) {
        return new Prediction(user, Source.SINGLE, null, null, f, s);
    }

    public static Prediction batchRow(User user, UUID batchId, int csvRow, ModelFeatures f, ModelScore s) {
        return new Prediction(user, Source.BATCH, batchId.toString(), csvRow, f, s);
    }

    private Prediction(User user, Source source, String batchId, Integer csvRow, ModelFeatures f, ModelScore s) {
        this.user = user;
        this.source = source;
        this.batchId = batchId;
        this.csvRow = csvRow;
        this.step = f.step();
        this.typeCode = f.typeCode();
        this.amount = f.amount();
        this.oldbalanceOrg = f.oldbalanceOrg();
        this.newbalanceOrig = f.newbalanceOrig();
        this.oldbalanceDest = f.oldbalanceDest();
        this.newbalanceDest = f.newbalanceDest();
        this.fraud = s.isFraud();
        this.probFraud = s.probFraud();
        this.threshold = s.threshold();
        List<String> reasons = s.reasons();
        this.reason1 = reasons.get(0);
        this.reason2 = reasons.get(1);
        this.reason3 = reasons.get(2);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Source getSource() { return source; }
    public String getBatchId() { return batchId; }
    public Integer getCsvRow() { return csvRow; }
    public int getStep() { return step; }
    public int getTypeCode() { return typeCode; }
    public double getAmount() { return amount; }
    public double getOldbalanceOrg() { return oldbalanceOrg; }
    public double getNewbalanceOrig() { return newbalanceOrig; }
    public double getOldbalanceDest() { return oldbalanceDest; }
    public double getNewbalanceDest() { return newbalanceDest; }
    public boolean isFraud() { return fraud; }
    public Double getProbFraud() { return probFraud; }
    public double getThreshold() { return threshold; }
    public String getReason1() { return reason1; }
    public String getReason2() { return reason2; }
    public String getReason3() { return reason3; }
    public Instant getCreatedAt() { return createdAt; }
}
