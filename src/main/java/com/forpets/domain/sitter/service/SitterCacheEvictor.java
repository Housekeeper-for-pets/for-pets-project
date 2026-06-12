package com.forpets.domain.sitter.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * 멤버 소유 필드(region/gender/nickname)가 시터 목록/상세 응답에 포함되어 있어,
 * 회원 정보 수정 시 시터 캐시가 stale 해지는 것을 막기 위한 무효화 전용 빈입니다.
 *
 * <p>{@link SitterCacheService}가 {@code MemberService}를 주입받고 있어, MemberService에서
 * SitterCacheService를 직접 호출하면 순환 참조(MemberService → SitterCacheService → MemberService)가
 * 발생합니다. 이를 피하기 위해 MemberService에 의존하지 않는 별도 빈으로 분리했습니다.
 *
 * <p>주의: {@code @CacheEvict}는 스프링 프록시를 통해서만 동작하므로, 호출부(MemberService)에서
 * 각 메서드를 <b>외부 호출</b>해야 합니다. 같은 클래스 내부에서 자기 호출하면 프록시가 우회되어
 * 무효화가 누락됩니다.
 */
@Component
public class SitterCacheEvictor {

    /**
     * 시터 상세 캐시 단건 무효화 (cache:sitter:{sitterId})
     */
    @CacheEvict(cacheNames = "sitter", key = "#sitterId", cacheManager = "longTtlCacheManager")
    public void evictSitterDetail(Long sitterId) {
        // 캐시 무효화만 수행 — 별도 로직 없음
    }

    /**
     * 시터 목록 캐시 전체 무효화 (cache:sitters:*)
     * region/gender가 캐시 키 구성요소이자 검색 필터라, 특정 키만 골라 지울 수 없어 전체 무효화합니다.
     */
    @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager")
    public void evictSitterList() {
        // 캐시 무효화만 수행 — 별도 로직 없음
    }
}
