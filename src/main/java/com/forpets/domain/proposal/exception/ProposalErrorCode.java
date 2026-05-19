package com.forpets.domain.proposal.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProposalErrorCode implements ErrorCode {

    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "존재하지 않는 제안입니다."),
    CANNOT_PROPOSE_OWN_POST(HttpStatus.BAD_REQUEST, "P006", "본인의 공고에 지원할 수 없습니다."),
    DUPLICATE_PROPOSAL(HttpStatus.BAD_REQUEST, "P007", "중복된 제안입니다."),
    NOT_PENDING_PROPOSAL(HttpStatus.BAD_REQUEST, "P008", "해당 요청은 제안이 대기상태 일 때 가능합니다."),
    NOT_PROPOSAL_PARTY(HttpStatus.FORBIDDEN, "P009", "제안 상세 조회는 공고 작성자 또는 제안한 시터만 가능합니다."),
    HAS_ACTIVE_PROPOSAL(HttpStatus.BAD_REQUEST, "P010", "대기 또는 채택된 제안이 존재합니다.")

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
