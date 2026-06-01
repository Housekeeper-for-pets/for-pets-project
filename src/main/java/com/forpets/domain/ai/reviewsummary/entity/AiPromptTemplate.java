package com.forpets.domain.ai.reviewsummary.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_prompt_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiPromptTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromptCategory category;

    @Column(nullable = false, length = 30)
    private String promptVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    @Column(nullable = false)
    private boolean active;

    @Builder
    private AiPromptTemplate(String feature, PromptCategory category, String promptVersion,
                             String template, boolean active) {
        this.feature = feature;
        this.category = category;
        this.promptVersion = promptVersion;
        this.template = template;
        this.active = active;
    }
}
