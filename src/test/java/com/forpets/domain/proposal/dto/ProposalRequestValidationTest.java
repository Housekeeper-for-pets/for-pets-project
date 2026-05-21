package com.forpets.domain.proposal.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ProposalRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("제안 등록 검증 — POST /api/proposals/posts/{postId}")
    class CreateProposalValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void proposal_validation_01() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(30000, "잘 돌봐드리겠습니다");

            // when
            Set<ConstraintViolation<CreateProposalRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] proposedPrice 누락 시 실패")
        void proposal_validation_02() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(null, "잘 돌봐드리겠습니다");

            // when
            Set<ConstraintViolation<CreateProposalRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("proposedPrice"));
        }

        @Test
        @DisplayName("[실패] proposedPrice 0 이하 입력 시 실패")
        void proposal_validation_03() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(0, "잘 돌봐드리겠습니다");

            // when
            Set<ConstraintViolation<CreateProposalRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("proposedPrice"));
        }

        @Test
        @DisplayName("[실패] proposedPrice 음수 입력 시 실패")
        void proposal_validation_04() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(-5000, "잘 돌봐드리겠습니다");

            // when
            Set<ConstraintViolation<CreateProposalRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("proposedPrice"));
        }

        @Test
        @DisplayName("[성공] message null 허용 — 선택 필드")
        void proposal_validation_05() {
            // given
            CreateProposalRequest request = new CreateProposalRequest(30000, null);

            // when
            Set<ConstraintViolation<CreateProposalRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }
}