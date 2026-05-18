package com.forpets.domain.sitter.repository;

import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import org.springframework.data.domain.Pageable;

/**
 * 시터 목록 동적 검색을 위한 QueryDSL 커스텀 리포지토리 인터페이스입니다.
 */
public interface SitterProfileRepositoryCustom {

    SitterPageResponse searchSitters(SitterSearchCondition condition, Pageable pageable);
}
