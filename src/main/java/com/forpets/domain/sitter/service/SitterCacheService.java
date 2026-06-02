package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SitterCacheService {

    private final SitterProfileRepository sitterProfileRepository;
    private final SitterScheduleRepository sitterScheduleRepository;
    private final MemberService memberService;

    @Cacheable(cacheNames = "sitters", keyGenerator = "sitterCacheKeyGenerator",
            cacheManager = "longTtlCacheManager")
    public SitterPageResponse searchSitters(SitterSearchCondition condition,
                                            int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        return sitterProfileRepository.searchSitters(condition, pageable);
    }

    /**
     * 시터 상세 조회 캐시
     * - 캐시 키: cache:sitter:{sitterId}  (computePrefixWith 규칙으로 자동 적용)
     * - TTL: 1시간 (longTtlCacheManager)
     * - 캐시 미스 시 DB 3회 조회(sitterProfile, member, schedules) 후 SitterResponseDto 저장
     */
    @Cacheable(cacheNames = "sitter", key = "#sitterId", cacheManager = "longTtlCacheManager")
    public SitterResponseDto getSitterById(Long sitterId) {
        SitterProfile sitter = sitterProfileRepository.findById(sitterId)
                .orElseThrow(() -> new SitterException(SitterErrorCode.SITTER_NOT_FOUND));
        Member member = memberService.findById(sitter.getMemberId());
        List<SitterSchedule> schedules = sitterScheduleRepository.findAllBySitterProfileId(sitter.getId());
        return SitterResponseDto.from(sitter, member.getRegion(), schedules);
    }

    /**
     * 시터 상세 캐시 단건 무효화
     * - SitterService에서 memberId만 알고 sitterId를 SpEL로 지정하기 어려운 경우 명시적으로 호출
     * - 트리거: 프로필 수정, 상태 변경, 삭제, 스케줄 변경
     */
    @CacheEvict(cacheNames = "sitter", key = "#sitterId", cacheManager = "longTtlCacheManager")
    public void evictSitterDetail(Long sitterId) {
        // 캐시 무효화만 수행 — 별도 로직 없음
    }
}
