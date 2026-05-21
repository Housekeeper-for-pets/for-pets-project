package com.forpets.domain.sitter.dto;

import com.forpets.domain.sitter.dto.profile.CreateSitterRequest;
import com.forpets.domain.sitter.dto.profile.UpdateSitterStatusRequest;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
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

class SitterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CreateSitterRequest validRequest() {
        return new CreateSitterRequest(
                "반려동물을 사랑하는 시터입니다", 3,
                PossiblePetType.ALL, PossiblePetSize.ALL, 15000
        );
    }

    @Nested
    @DisplayName("시터 프로필 등록 검증 — POST /api/sitters")
    class CreateValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void sitter_validation_01() {
            // given
            CreateSitterRequest request = validRequest();

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] 경력 연수 누락 시 등록 실패")
        void sitter_validation_02() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", null,
                    PossiblePetType.ALL, PossiblePetSize.ALL, 15000
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("experienceYears"));
        }

        @Test
        @DisplayName("[실패] 돌봄 가능 반려동물 타입 누락 시 등록 실패")
        void sitter_validation_03() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", 3,
                    null, PossiblePetSize.ALL, 15000
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("possiblePetType"));
        }

        @Test
        @DisplayName("[실패] 돌봄 가능 반려동물 크기 누락 시 등록 실패")
        void sitter_validation_04() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", 3,
                    PossiblePetType.ALL, null, 15000
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("possiblePetSize"));
        }

        @Test
        @DisplayName("[실패] 시간당 요금 누락 시 등록 실패")
        void sitter_validation_05() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", 3,
                    PossiblePetType.ALL, PossiblePetSize.ALL, null
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pricePerHour"));
        }

        @Test
        @DisplayName("[실패] 시간당 요금 0 이하 입력 시 등록 실패")
        void sitter_validation_06() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", 3,
                    PossiblePetType.ALL, PossiblePetSize.ALL, 0
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pricePerHour"));
        }

        @Test
        @DisplayName("[실패] 시간당 요금 음수 입력 시 등록 실패")
        void sitter_validation_07() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소개글", 3,
                    PossiblePetType.ALL, PossiblePetSize.ALL, -5000
            );

            // when
            Set<ConstraintViolation<CreateSitterRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pricePerHour"));
        }
    }

    @Nested
    @DisplayName("시터 상태 변경 검증 — PATCH /api/sitters/me/status")
    class UpdateStatusValidationTest {

        @Test
        @DisplayName("[실패] 시터 상태 누락 시 변경 실패")
        void sitter_validation_08() {
            // given
            UpdateSitterStatusRequest request = new UpdateSitterStatusRequest(null);

            // when
            Set<ConstraintViolation<UpdateSitterStatusRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
        }
    }
}