package com.forpets.domain.sitter.service;

import com.forpets.domain.sitter.dto.schedule.ScheduleItemRequest;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.dto.schedule.UpdateScheduleRequest;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.embed.exception.TimeSlotErrorCode;
import com.forpets.global.embed.exception.TimeSlotException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SitterScheduleServiceTest {

    @InjectMocks
    private SitterScheduleService sitterScheduleService;

    @Mock
    private SitterScheduleRepository sitterScheduleRepository;

    @Mock
    private SitterService sitterService;

    // ── 테스트 픽스처 ──
    private SitterProfile sitterProfile;

    private final Long member1Id = 1L;
    private final Long member2Id = 2L;
    private final Long sitterProfileId = 100L;

    @BeforeEach
    void setUp() {
        // member1의 시터 프로필
        sitterProfile = SitterProfile.builder()
                .memberId(member1Id)
                .introduction("반려동물을 사랑하는 시터입니다")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.ALL)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterProfileId);
    }

    // ========================================================
    // 스케줄 전체 교체 — PUT /api/sitters/me/schedules
    // ========================================================
    @Nested
    @DisplayName("스케줄 전체 교체 — PUT /api/sitters/me/schedules")
    class ReplaceAllTest {

        @Test
        @DisplayName("[성공] 스케줄 전체 교체 성공 — 기존 삭제 후 신규 삽입")
        void schedule_test_01() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(15, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            SitterSchedule schedule1 = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            ReflectionTestUtils.setField(schedule1, "id", 1L);

            SitterSchedule schedule2 = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.WEDNESDAY)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(15, 0))
                    .build();
            ReflectionTestUtils.setField(schedule2, "id", 2L);

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);
            given(sitterScheduleRepository.saveAll(anyList())).willReturn(List.of(schedule1, schedule2));

            // when
            List<ScheduleResponseDto> result = sitterScheduleService.replaceAll(member1Id, request);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.get(0).endTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(result.get(1).dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
            then(sitterScheduleRepository).should().deleteAllBySitterProfileId(sitterProfileId);
            then(sitterScheduleRepository).should().flush();
            then(sitterScheduleRepository).should().saveAll(anyList());
        }

        @Test
        @DisplayName("[성공] 빈 리스트 전송 시 전체 삭제 (모든 요일 OFF)")
        void schedule_test_02() {
            // given
            UpdateScheduleRequest request = new UpdateScheduleRequest(List.of());

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);
            given(sitterScheduleRepository.saveAll(anyList())).willReturn(List.of());

            // when
            List<ScheduleResponseDto> result = sitterScheduleService.replaceAll(member1Id, request);

            // then
            assertThat(result).isEmpty();
            then(sitterScheduleRepository).should().deleteAllBySitterProfileId(sitterProfileId);
            then(sitterScheduleRepository).should().flush();
        }

        @Test
        @DisplayName("[성공] 7개 요일 전체 등록 성공")
        void schedule_test_03() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                    new ScheduleItemRequest(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(16, 0)),
                    new ScheduleItemRequest(DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            List<SitterSchedule> savedSchedules = items.stream()
                    .map(item -> {
                        SitterSchedule s = SitterSchedule.builder()
                                .sitterProfileId(sitterProfileId)
                                .dayOfWeek(item.dayOfWeek())
                                .startTime(item.startTime())
                                .endTime(item.endTime())
                                .build();
                        ReflectionTestUtils.setField(s, "id", (long) (item.dayOfWeek().getValue()));
                        return s;
                    })
                    .toList();

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);
            given(sitterScheduleRepository.saveAll(anyList())).willReturn(savedSchedules);

            // when
            List<ScheduleResponseDto> result = sitterScheduleService.replaceAll(member1Id, request);

            // then
            assertThat(result).hasSize(7);
        }

        @Test
        @DisplayName("[실패] 시작 시간이 종료 시간보다 늦은 경우")
        void schedule_test_04() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(9, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> sitterScheduleService.replaceAll(member1Id, request))
                    .isInstanceOf(TimeSlotException.class)
                    .satisfies(ex -> assertThat(((TimeSlotException) ex).getErrorCode())
                            .isEqualTo(TimeSlotErrorCode.INVALID_TIME_RANGE));
        }

        @Test
        @DisplayName("[실패] 시작 시간과 종료 시간이 같은 경우")
        void schedule_test_05() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(12, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> sitterScheduleService.replaceAll(member1Id, request))
                    .isInstanceOf(TimeSlotException.class)
                    .satisfies(ex -> assertThat(((TimeSlotException) ex).getErrorCode())
                            .isEqualTo(TimeSlotErrorCode.INVALID_TIME_RANGE));
        }

        @Test
        @DisplayName("[실패] 같은 요일이 중복 등록된 경우")
        void schedule_test_06() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> sitterScheduleService.replaceAll(member1Id, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.DUPLICATE_SCHEDULE));
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 스케줄 등록 시도")
        void schedule_test_07() {
            // given
            List<ScheduleItemRequest> items = List.of(
                    new ScheduleItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))
            );
            UpdateScheduleRequest request = new UpdateScheduleRequest(items);

            given(sitterService.findApprovedByMemberId(member2Id))
                    .willThrow(new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> sitterScheduleService.replaceAll(member2Id, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
        }
    }

    // ========================================================
    // 내 스케줄 조회 — GET /api/sitters/me/schedules
    // ========================================================
    @Nested
    @DisplayName("내 스케줄 조회 — GET /api/sitters/me/schedules")
    class GetSchedulesTest {

        @Test
        @DisplayName("[성공] 내 스케줄 목록 조회 성공")
        void schedule_test_08() {
            // given
            SitterSchedule schedule1 = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            ReflectionTestUtils.setField(schedule1, "id", 1L);

            SitterSchedule schedule2 = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.FRIDAY)
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(16, 0))
                    .build();
            ReflectionTestUtils.setField(schedule2, "id", 2L);

            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);
            given(sitterScheduleRepository.findAllBySitterProfileId(sitterProfileId))
                    .willReturn(List.of(schedule1, schedule2));

            // when
            List<ScheduleResponseDto> result = sitterScheduleService.getSchedules(member1Id);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.get(0).endTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(result.get(1).dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        }

        @Test
        @DisplayName("[성공] 등록된 스케줄 없을 때 빈 리스트 반환")
        void schedule_test_09() {
            // given
            given(sitterService.findApprovedByMemberId(member1Id)).willReturn(sitterProfile);
            given(sitterScheduleRepository.findAllBySitterProfileId(sitterProfileId))
                    .willReturn(List.of());

            // when
            List<ScheduleResponseDto> result = sitterScheduleService.getSchedules(member1Id);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 조회 시도")
        void schedule_test_10() {
            // given
            given(sitterService.findApprovedByMemberId(member2Id))
                    .willThrow(new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> sitterScheduleService.getSchedules(member2Id))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
        }
    }
}