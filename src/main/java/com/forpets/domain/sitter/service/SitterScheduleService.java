package com.forpets.domain.sitter.service;

import com.forpets.domain.sitter.dto.schedule.ScheduleItemRequest;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.dto.schedule.UpdateScheduleRequest;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterScheduleService {
    private final SitterScheduleRepository sitterScheduleRepository;
    private final SitterService sitterService;

    /*
    시터 가능 시간 전체 교체
    1. 요청 목록 유효성 검증 (시간 역전, 요일 중복)
    2. AVAILABLE 상태 스케줄만 삭제 (BOOKED 상태는 유지)
    3. 신규 스케줄 전체 삽입

    BOOKED 상태 스케줄이 존재하는 요일에 대해 새 스케줄이 들어오면
    DB Unique 제약에 의해 예외 발생 → 409 DUPLICATE_SCHEDULE
 */
    @Transactional
    public List<ScheduleResponseDto> replaceAll(Long memberId, UpdateScheduleRequest request) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);

        validateScheduleItems(request.schedules());

        // AVAILABLE 상태만 삭제 (BOOKED는 보존)
        sitterScheduleRepository.deleteAllBySitterProfileId(sitter.getId());
        sitterScheduleRepository.flush();

        List<SitterSchedule> newSchedules = request.schedules().stream()
                .map(item -> SitterSchedule.builder()
                        .sitterProfileId(sitter.getId())
                        .dayOfWeek(item.dayOfWeek())
                        .startTime(item.startTime())
                        .endTime(item.endTime())
                        .build())
                .toList();

        return sitterScheduleRepository.saveAll(newSchedules).stream()
                .map(ScheduleResponseDto::from)
                .toList();
    }


    public List<ScheduleResponseDto> getSchedules(Long sitterId) {
        // 시터 존재 여부 확인
        sitterService.findByMemberId(sitterId);

        return sitterScheduleRepository.findAllBySitterProfileId(sitterId).stream()
                .map(ScheduleResponseDto::from)
                .toList();
    }

    // private ------------
    private void validateScheduleItems(List<ScheduleItemRequest> items) {
        Set<DayOfWeek> days = new HashSet<>();

        for (ScheduleItemRequest item : items) {
            if (!item.startTime().isBefore(item.endTime())) {
                throw new BusinessException(CommonErrorCode.INVALID_TIME_RANGE);
            }

            if (!days.add(item.dayOfWeek())) {
                throw new BusinessException(CommonErrorCode.DUPLICATE_SCHEDULE);
            }
        }
    }
}
