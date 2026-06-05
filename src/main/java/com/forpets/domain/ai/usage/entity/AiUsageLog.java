package com.forpets.domain.ai.usage.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_usage_logs")
public class AiUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiFeature feature;

    @Column(length = 100)
    private String model;

    @Column
    private Integer promptTokens;

    @Column
    private Integer completionTokens;

    @Column
    private Integer totalTokens;

    @Column(nullable = false)
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiUsageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AiErrorType errorType;

    @Column(length = 100)
    private String promptVersion;

    @Column(length = 500)
    private String failureReason;

    private AiUsageLog(
            AiFeature feature,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Long latencyMs,
            AiUsageStatus status,
            AiErrorType errorType,
            String promptVersion,
            String failureReason
    ) {
        this.feature = feature;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.status = status;
        this.errorType = errorType;
        this.promptVersion = promptVersion;
        this.failureReason = failureReason;
    }

    public static AiUsageLog create(
            AiFeature feature,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Long latencyMs,
            AiUsageStatus status,
            AiErrorType errorType,
            String promptVersion,
            String failureReason
    ) {
        return new AiUsageLog(
                feature,
                model,
                promptTokens,
                completionTokens,
                totalTokens,
                latencyMs,
                status,
                errorType,
                promptVersion,
                failureReason
        );
    }
}
