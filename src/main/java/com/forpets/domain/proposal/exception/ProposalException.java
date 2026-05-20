package com.forpets.domain.proposal.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class ProposalException extends BusinessException {
    public ProposalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProposalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
