package com.forpets.domain.ai.rag.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiRagErrorCode implements ErrorCode {

    RAG_INDEX_FAILED(HttpStatus.BAD_REQUEST, "RAG_INDEX_FAILED", "RAG 인덱싱에 실패했습니다."),
    RAG_SEARCH_FAILED(HttpStatus.BAD_REQUEST, "RAG_SEARCH_FAILED", "RAG 검색에 실패했습니다."),
    RAG_EMBEDDING_FAILED(HttpStatus.BAD_REQUEST, "RAG_EMBEDDING_FAILED", "RAG 임베딩 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
