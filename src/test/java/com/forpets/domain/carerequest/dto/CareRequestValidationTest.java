package com.forpets.domain.carerequest.dto;

import com.forpets.global.common.CareType;
import com.forpets.global.embed.dto.TimeSlotRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CareRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private List<TimeSlotRequest> validTimeSlots() {
        return List.of(
                new TimeSlotRequest(
                        LocalDate.now().plusDays(3),
                        LocalTime.of(10, 0),
                        LocalTime.of(14, 0)
                )
        );
    }

    private CreateCareRequestDto validRequest() {
        return new CreateCareRequestDto(
                List.of(1L), CareType.VISIT, "돌봐주세요",
                validTimeSlots(), 30000
        );
    }

    @Nested
    @DisplayName("케어 요청 등록 검증 — POST /api/care-requests/sitters/{sitterId}")
    class CreateCareRequestValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void carerequest_validation_01() {
            // given
            CreateCareRequestDto request = validRequest();

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] petIds 누락(null) 시 실패")
        void carerequest_validation_02() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    null, CareType.VISIT, "돌봐주세요",
                    validTimeSlots(), 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("petIds"));
        }

        @Test
        @DisplayName("[실패] petIds 빈 리스트 시 실패")
        void carerequest_validation_03() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(), CareType.VISIT, "돌봐주세요",
                    validTimeSlots(), 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("petIds"));
        }

        @Test
        @DisplayName("[실패] careType 누락 시 실패")
        void carerequest_validation_04() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), null, "돌봐주세요",
                    validTimeSlots(), 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("careType"));
        }

        @Test
        @DisplayName("[실패] timeSlots 누락(null) 시 실패")
        void carerequest_validation_05() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    null, 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("timeSlots"));
        }

        @Test
        @DisplayName("[실패] timeSlots 빈 리스트 시 실패")
        void carerequest_validation_06() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    List.of(), 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("timeSlots"));
        }

        @Test
        @DisplayName("[실패] requestPrice 0 입력 시 실패")
        void carerequest_validation_07() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    validTimeSlots(), 0
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestPrice"));
        }

        @Test
        @DisplayName("[실패] requestPrice 음수 입력 시 실패")
        void carerequest_validation_08() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    validTimeSlots(), -5000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestPrice"));
        }

        @Test
        @DisplayName("[성공] message null 허용 — 선택 필드")
        void carerequest_validation_09() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, null,
                    validTimeSlots(), 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("TimeSlot 내부 검증 — @Valid 전파")
    class TimeSlotValidationTest {

        @Test
        @DisplayName("[실패] TimeSlot의 careDate 누락 시 실패")
        void carerequest_validation_10() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(null, LocalTime.of(10, 0), LocalTime.of(14, 0))
            );
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    invalidSlots, 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("careDate"));
        }

        @Test
        @DisplayName("[실패] TimeSlot의 startTime 누락 시 실패")
        void carerequest_validation_11() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(LocalDate.now().plusDays(3), null, LocalTime.of(14, 0))
            );
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    invalidSlots, 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("startTime"));
        }

        @Test
        @DisplayName("[실패] TimeSlot의 endTime 누락 시 실패")
        void carerequest_validation_12() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(LocalDate.now().plusDays(3), LocalTime.of(10, 0), null)
            );
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(1L), CareType.VISIT, "돌봐주세요",
                    invalidSlots, 30000
            );

            // when
            Set<ConstraintViolation<CreateCareRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("endTime"));
        }
    }
}