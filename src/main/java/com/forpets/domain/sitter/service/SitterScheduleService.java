package com.forpets.domain.sitter.service;

import com.forpets.domain.sitter.dto.schedule.ScheduleItemRequest;
import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.dto.schedule.UpdateScheduleRequest;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.embed.exception.TimeSlotErrorCode;
import com.forpets.global.embed.exception.TimeSlotException;
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
    2. 기존 스케줄 다 삭제
    3. 신규 스케줄 전체 삽입
    */
    @Transactional
    public List<ScheduleResponseDto> replaceAll(Long memberId, UpdateScheduleRequest request) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);

        validateScheduleItems(request.schedules());

        sitterScheduleRepository.deleteAllBySitterProfileId(sitter.getId());
        sitterScheduleRepository.flush();
        // Delete 와 saveAll 이 순서 보장이 안 될 가능성이 있기 때문에
        // flush 로 보내주기

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

    public List<ScheduleResponseDto> getSchedules(Long memberId) {
        // 시터 존재 여부 확인
        SitterProfile sitterProfile = sitterService.findByMemberId(memberId);

        return sitterScheduleRepository.findAllBySitterProfileId(sitterProfile.getId()).stream()
                .map(ScheduleResponseDto::from)
                .toList();
    }

    // private ------------
    private void validateScheduleItems(List<ScheduleItemRequest> items) {
        Set<DayOfWeek> days = new HashSet<>();

        for (ScheduleItemRequest item : items) {
            if (!item.startTime().isBefore(item.endTime())) {
                throw new TimeSlotException(TimeSlotErrorCode.INVALID_TIME_RANGE);
            }

            if (!days.add(item.dayOfWeek())) {
                throw new SitterException(SitterErrorCode.DUPLICATE_SCHEDULE);
            }
        }
    }
}
