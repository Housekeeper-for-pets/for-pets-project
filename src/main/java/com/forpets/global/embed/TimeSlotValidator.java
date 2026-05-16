package com.forpets.global.embed;

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

    private void validateNotEmpty(List<TimeSlotRequest> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new BusinessException(CommonErrorCode.TIME_SLOT_REQUIRED);
        }
    }

    private void validateMaxCount(List<TimeSlotRequest> timeSlots) {
        if (timeSlots.size() > MAX_TIME_SLOT_COUNT) {
            throw new BusinessException(CommonErrorCode.TIMESLOT_LIMIT_EXCEEDED);
        }
    }

    private void validateEachSlot(List<TimeSlotRequest> timeSlots) {
        LocalDate today = LocalDate.now();

        for (TimeSlotRequest slot : timeSlots) {
            if (slot.careDate().isBefore(today)) {
                throw new BusinessException(CommonErrorCode.PAST_DATE_NOT_ALLOWED);
            }
            if (!slot.startTime().isBefore(slot.endTime())) {
                throw new BusinessException(CommonErrorCode.INVALID_TIME_RANGE);
            }
        }
    }

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
                    throw new BusinessException(CommonErrorCode.DUPLICATE_TIME_SLOT);
                }
            }
        }
    }
}