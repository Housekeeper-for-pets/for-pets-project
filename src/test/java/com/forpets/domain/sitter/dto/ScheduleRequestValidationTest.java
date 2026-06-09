package com.forpets.domain.sitter.dto;

import com.forpets.domain.sitter.dto.schedule.ScheduleItemRequest;
import com.forpets.domain.sitter.dto.schedule.UpdateScheduleRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ScheduleRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("스케줄 전체 교체 검증 — PUT /api/sitters/me/schedules")
    class UpdateScheduleValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void schedule_validation_01() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(15, 0))
            ));

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] schedules 목록 null 시 실패")
        void schedule_validation_02() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(null);

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("schedules"));
        }

        @Test
        @DisplayName("[실패] schedules 8개 초과 시 실패")
        void schedule_validation_03() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("schedules"));
        }
    }

    @Nested
    @DisplayName("스케줄 항목 검증 — ScheduleItemRequest")
    class ScheduleItemValidationTest {

        @Test
        @DisplayName("[실패] 요일 누락 시 실패")
        void schedule_validation_04() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(List.of(
                    new ScheduleItemRequest(null, LocalTime.of(9, 0), LocalTime.of(18, 0))
            ));

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("dayOfWeek"));
        }

        @Test
        @DisplayName("[실패] 시작 시간 누락 시 실패")
        void schedule_validation_05() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, null, LocalTime.of(18, 0))
            ));

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("startTime"));
        }

        @Test
        @DisplayName("[실패] 종료 시간 누락 시 실패")
        void schedule_validation_06() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), null)
            ));

            // when
            Set<ConstraintViolation<UpdateScheduleRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("endTime"));
        }
    }
}