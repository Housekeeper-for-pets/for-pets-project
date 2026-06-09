package com.forpets.domain.ai.reviewsummary.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class AiReviewSummaryException extends BusinessException {

    public AiReviewSummaryException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AiReviewSummaryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
