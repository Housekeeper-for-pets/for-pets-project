package com.forpets.domain.post.dto;

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

class PostRequestValidationTest {

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

    private CreatePostRequest validRequest() {
        return new CreatePostRequest(
                "타코 돌봐주실 분 구합니다",
                "3일간 출장이라 돌봐주실 분 찾아요",
                List.of(1L),
                CareType.VISIT,
                30000,
                validTimeSlots()
        );
    }

    @Nested
    @DisplayName("공고 등록/수정 검증 — POST /api/posts, PUT /api/posts/{postId}")
    class CreateUpdateValidationTest {

        @Test
        @DisplayName("[성공] 유효한 요청 — 검증 통과")
        void post_validation_01() {
            // given
            CreatePostRequest request = validRequest();

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("[실패] title 누락 시 실패")
        void post_validation_02() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    null, "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        }

        @Test
        @DisplayName("[실패] title 공백만 입력 시 실패")
        void post_validation_03() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "   ", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        }

        @Test
        @DisplayName("[실패] content 누락 시 실패")
        void post_validation_04() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", null, List.of(1L),
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        @DisplayName("[실패] content 공백만 입력 시 실패")
        void post_validation_05() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "   ", List.of(1L),
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        @DisplayName("[실패] petIds null 시 실패")
        void post_validation_06() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", null,
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("petIds"));
        }

        @Test
        @DisplayName("[실패] petIds 빈 리스트 시 실패")
        void post_validation_07() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(),
                    CareType.VISIT, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("petIds"));
        }

        @Test
        @DisplayName("[실패] careType 누락 시 실패")
        void post_validation_08() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    null, 30000, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("careType"));
        }

        @Test
        @DisplayName("[실패] budgetAmount 0 이하 입력 시 실패")
        void post_validation_09() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 0, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("budgetAmount"));
        }

        @Test
        @DisplayName("[실패] timeSlots null 시 실패")
        void post_validation_10() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, null
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("timeSlots"));
        }

        @Test
        @DisplayName("[실패] timeSlots 빈 리스트 시 실패")
        void post_validation_11() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, List.of()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("timeSlots"));
        }

        @Test
        @DisplayName("[성공] budgetAmount null 허용 — 선택 필드")
        void post_validation_12() {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, null, validTimeSlots()
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("TimeSlot 내부 검증 — @Valid 전파")
    class TimeSlotValidationTest {

        @Test
        @DisplayName("[실패] TimeSlot의 careDate 누락 시 실패")
        void post_validation_13() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(null, LocalTime.of(10, 0), LocalTime.of(14, 0))
            );
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, invalidSlots
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("careDate"));
        }

        @Test
        @DisplayName("[실패] TimeSlot의 startTime 누락 시 실패")
        void post_validation_14() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(LocalDate.now().plusDays(3), null, LocalTime.of(14, 0))
            );
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, invalidSlots
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("startTime"));
        }

        @Test
        @DisplayName("[실패] TimeSlot의 endTime 누락 시 실패")
        void post_validation_15() {
            // given
            List<TimeSlotRequest> invalidSlots = List.of(
                    new TimeSlotRequest(LocalDate.now().plusDays(3), LocalTime.of(10, 0), null)
            );
            CreatePostRequest request = new CreatePostRequest(
                    "제목입니다", "내용입니다", List.of(1L),
                    CareType.VISIT, 30000, invalidSlots
            );

            // when
            Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("endTime"));
        }
    }
}