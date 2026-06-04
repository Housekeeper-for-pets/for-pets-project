package com.forpets.domain.ai.reviewsummary.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiReviewSummaryErrorCode implements ErrorCode {

    REVIEW_SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_SUMMARY_NOT_FOUND", "리뷰 요약을 찾을 수 없습니다."),
    REVIEW_SOURCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "REVIEW_SOURCE_NOT_FOUND", "요약할 리뷰가 없습니다."),
    PROMPT_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT_TEMPLATE_NOT_FOUND", "활성화된 프롬프트 템플릿이 없습니다."),
    INVALID_AI_RESPONSE(HttpStatus.BAD_REQUEST, "INVALID_AI_RESPONSE", "AI 응답 형식이 올바르지 않습니다."),
    AI_REVIEW_SUMMARY_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI_REVIEW_SUMMARY_RATE_LIMITED", "AI 리뷰 요약 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    AI_REVIEW_SUMMARY_FAILED(HttpStatus.BAD_REQUEST, "AI_REVIEW_SUMMARY_FAILED", "AI 리뷰 요약 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
