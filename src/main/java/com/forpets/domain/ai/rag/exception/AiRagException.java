package com.forpets.domain.ai.rag.exception;

import com.forpets.global.exception.BusinessException;

public class AiRagException extends BusinessException {

    public AiRagException(AiRagErrorCode errorCode) {
        super(errorCode);
    }

    public AiRagException(AiRagErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
