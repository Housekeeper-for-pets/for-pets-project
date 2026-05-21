package com.forpets.domain.reservation.dto;


import com.forpets.domain.reservation.entity.CancelCategory;
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

class ReservationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("예약 취소 요청 검증 — PATCH /api/reservations/{reservationId}/cancel")
    class CancelValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void reservation_validation_01() {
            // given
            CancelReservationRequest request = new CancelReservationRequest(
                    "개인 사정으로 인해 취소합니다", CancelCategory.PERSONAL
            );

            // when
            Set<ConstraintViolation<CancelReservationRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] cancelReason 누락 시 실패")
        void reservation_validation_02() {
            // given
            CancelReservationRequest request = new CancelReservationRequest(
                    null, CancelCategory.PERSONAL
            );

            // when
            Set<ConstraintViolation<CancelReservationRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cancelReason"));
        }

        @Test
        @DisplayName("[실패] cancelReason 공백만 입력 시 실패")
        void reservation_validation_03() {
            // given
            CancelReservationRequest request = new CancelReservationRequest(
                    "          ", CancelCategory.PERSONAL
            );

            // when
            Set<ConstraintViolation<CancelReservationRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cancelReason"));
        }

        @Test
        @DisplayName("[실패] cancelReason 10자 미만 입력 시 실패")
        void reservation_validation_04() {
            // given
            CancelReservationRequest request = new CancelReservationRequest(
                    "짧은사유", CancelCategory.PERSONAL
            );

            // when
            Set<ConstraintViolation<CancelReservationRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cancelReason"));
        }

        @Test
        @DisplayName("[실패] cancelCategory 누락 시 실패")
        void reservation_validation_05() {
            // given
            CancelReservationRequest request = new CancelReservationRequest(
                    "개인 사정으로 인해 취소합니다", null
            );

            // when
            Set<ConstraintViolation<CancelReservationRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cancelCategory"));
        }
    }
}