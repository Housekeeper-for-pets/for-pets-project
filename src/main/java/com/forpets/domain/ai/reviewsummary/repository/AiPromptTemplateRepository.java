package com.forpets.domain.ai.reviewsummary.repository;

import com.forpets.domain.ai.reviewsummary.entity.AiPromptTemplate;
import com.forpets.domain.ai.reviewsummary.entity.PromptCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {

    Optional<AiPromptTemplate> findFirstByFeatureAndCategoryAndActiveTrueOrderByIdDesc(
            String feature,
            PromptCategory category
    );
}
