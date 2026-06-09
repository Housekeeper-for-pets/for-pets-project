package com.forpets.domain.pet.dto;

import com.forpets.domain.pet.dto.CreatePetRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.forpets.domain.pet.entity.PetSpecies;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class PetRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CreatePetRequest validRequest() {
        return new CreatePetRequest(
                "타코", PetSpecies.CAT, "코리안숏헤어", null,
                4, null, null, null
        );
    }

    @Nested
    @DisplayName("반려동물 등록 검증 — POST /api/pets")
    class CreateValidationTest {

        @Test
        @DisplayName("[실패] 반려동물 이름 누락 시 등록 실패")
        void pet_validation_01() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    null, PetSpecies.CAT, null, null,
                    null, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("[실패] 반려동물 이름 공백만 입력 시 등록 실패")
        void pet_validation_02() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "   ", PetSpecies.CAT, null, null,
                    null, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("[실패] species 누락 시 등록 실패")
        void pet_validation_03() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "타코", null, null, null,
                    null, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("species"));
        }

        @Test
        @DisplayName("[실패] 이름 50자 초과 등록")
        void pet_validation_04() {
            // given
            String longName = "가".repeat(51);
            CreatePetRequest request = new CreatePetRequest(
                    longName, PetSpecies.DOG, null, null,
                    null, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("[실패] 나이 음수 입력")
        void pet_validation_05() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "타코", PetSpecies.CAT, null, null,
                    -1, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
        }

        @Test
        @DisplayName("[실패] 나이 100 초과 입력")
        void pet_validation_06() {
            // given
            CreatePetRequest request = new CreatePetRequest(
                    "타코", PetSpecies.CAT, null, null,
                    101, null, null, null
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
        }

        @Test
        @DisplayName("[실패] 메모 2000자 초과 입력")
        void pet_validation_07() {
            // given
            String longNote = "가".repeat(2001);
            CreatePetRequest request = new CreatePetRequest(
                    "타코", PetSpecies.CAT, null, null,
                    null, null, null, longNote
            );

            // when
            Set<ConstraintViolation<CreatePetRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("note"));
        }
    }
}