package com.forpets.global.sse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventType {
    CARE_LOG("케어 일지가 등록되었습니다."),
    PROPOSAL_ARRIVED("새로운 제안이 도착했습니다."),
    MATCHING_CONFIRMED("매칭이 확정되었습니다."),
    REQUEST_RECEIVED("새로운 케어 신청이 도착했습니다."),
    PROPOSAL_WITHDRAWN("제안이 자동 철회되었습니다."),
    PAYMENT_COMPLETED("결제가 완료되었습니다."),
    SITTER_PROFILE_APPROVED("시터 프로필이 승인되었습니다."),
    RESERVATION_CANCELED("예약이 취소되었습니다."),
    RESERVATION_EXPIRED("예약이 만료되었습니다.");

    private final String defaultMessage;
}
