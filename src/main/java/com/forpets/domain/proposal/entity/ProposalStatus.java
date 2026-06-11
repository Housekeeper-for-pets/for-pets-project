package com.forpets.domain.proposal.entity;

public enum ProposalStatus {
    PENDING,
    ACCEPTED,
    REJECTED, // 공고 작성자 (반려인) 이 제안을 거절
    WITHDRAWN, // 시터 본인이 제안을 취소
    EXPIRED // 공고가 만료되어 더 이상 수락 불가
}
