package com.forpets.global.embed;

import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.embed.exception.TimeSlotErrorCode;
import com.forpets.global.embed.exception.TimeSlotException;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
TimeSlot 유효성 검증 컴포넌트
Post랑 CareRequest 등록, 수정 시 공통으로 사용

- 비어있지 않고
- 최대 개수 (30개) 를 넘지 않고
- 과거 날짜가 timeSlot 에 포함되지 않고
- 시작 이후 종료
- 겹치는 timeslot 이 있는지
검증

추가로 검증 내용이 생각나면 여기다가 추가하기
 */
@Component
public class TimeSlotValidator {

    private static final int MAX_TIME_SLOT_COUNT = 30;

    public void validate(List<TimeSlotRequest> timeSlots) {
        validateNotEmpty(timeSlots);
        validateMaxCount(timeSlots);
        validateEachSlot(timeSlots);
        validateNoOverlap(timeSlots);
    }

    // 1. TimeSlot list 가 비어있지 않은지
    private void validateNotEmpty(List<TimeSlotRequest> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new TimeSlotException(TimeSlotErrorCode.TIME_SLOT_REQUIRED);
        }
    }

    // 2. MaxCount 를 넘지 않았는지
    private void validateMaxCount(List<TimeSlotRequest> timeSlots) {
        if (timeSlots.size() > MAX_TIME_SLOT_COUNT) {
            throw new TimeSlotException(TimeSlotErrorCode.TIMESLOT_LIMIT_EXCEEDED);
        }
    }

    // 3. 각 TimeSlot 이 과거 날짜가 아니고, 시작시각이 종료 시각보다 이른지
    private void validateEachSlot(List<TimeSlotRequest> timeSlots) {
        LocalDate today = LocalDate.now();

        for (TimeSlotRequest slot : timeSlots) {
            if (slot.careDate().isBefore(today)) {
                throw new TimeSlotException(TimeSlotErrorCode.PAST_DATE_NOT_ALLOWED);
            }
            if (!slot.startTime().isBefore(slot.endTime())) {
                throw new TimeSlotException(TimeSlotErrorCode.INVALID_TIME_RANGE);
            }
        }
    }

    // 4. 각 TimeSlot 끼리 겹치지 않는지
    private void validateNoOverlap(List<TimeSlotRequest> timeSlots) {
        // 같은 날짜끼리 그룹핑
        Map<LocalDate, List<TimeSlotRequest>> grouped = timeSlots.stream()
                .collect(Collectors.groupingBy(TimeSlotRequest::careDate));

        // 같은 날짜의 슬롯을 시작 시간 순으로 정렬
        for (List<TimeSlotRequest> slotsOnSameDate : grouped.values()) {
            List<TimeSlotRequest> sorted = slotsOnSameDate.stream()
                    .sorted(Comparator.comparing(TimeSlotRequest::startTime))
                    .toList();

            for (int i = 1; i < sorted.size(); i++) {
                TimeSlotRequest prev = sorted.get(i - 1);
                TimeSlotRequest curr = sorted.get(i);

                // 앞 슬롯 종료 시간 > 뒤 슬롯 시작 시간 이면 예외처리
                if (prev.endTime().isAfter(curr.startTime())) {
                    throw new TimeSlotException(TimeSlotErrorCode.DUPLICATE_TIME_SLOT);
                }
            }
        }
    }

    /*
    두 TimeSlot 리스트 간 시간 겹침 여부 확인
    겹침 조건: 같은 날짜 AND 시작 < 기존종료 AND 종료 > 기존시작
     */
    public boolean hasTimeConflict(List<? extends HasTimeSlotInfo> slotsA,
                                   List<? extends HasTimeSlotInfo> slotsB) {
        for (HasTimeSlotInfo a : slotsA) {
            TimeSlotInfo infoA = a.getTimeSlotInfo();
            for (HasTimeSlotInfo b : slotsB) {
                TimeSlotInfo infoB = b.getTimeSlotInfo();

                if (infoA.getCareDate().equals(infoB.getCareDate())
                        && infoA.getStartTime().isBefore(infoB.getEndTime())
                        && infoA.getEndTime().isAfter(infoB.getStartTime())) {
                    return true;
                }
            }
        }
        return false;
    }
}