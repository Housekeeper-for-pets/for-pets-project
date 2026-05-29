package com.forpets.domain.proposal.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProposalErrorCode implements ErrorCode {

    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "존재하지 않는 제안입니다."),
    CANNOT_PROPOSE_OWN_POST(HttpStatus.BAD_REQUEST, "CANNOT_PROPOSE_OWN_POST", "본인의 공고에 지원할 수 없습니다."),
    DUPLICATE_PROPOSAL(HttpStatus.BAD_REQUEST, "DUPLICATE_PROPOSAL", "중복된 제안입니다."),
    NOT_PENDING_PROPOSAL(HttpStatus.BAD_REQUEST, "NOT_PENDING_PROPOSAL", "해당 요청은 제안이 대기상태 일 때 가능합니다."),
    NOT_PROPOSAL_PARTY(HttpStatus.FORBIDDEN, "NOT_PROPOSAL_PARTY", "해당 제안에 대한 권한이 없습니다."),
    HAS_ACTIVE_PROPOSAL(HttpStatus.BAD_REQUEST, "HAS_ACTIVE_PROPOSAL", "대기 또는 채택된 제안이 존재합니다."),
    NOT_PROPOSAL_OWNER(HttpStatus.FORBIDDEN, "NOT_PROPOSAL_OWNER", "본인의 제안이 아닙니다.")

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
