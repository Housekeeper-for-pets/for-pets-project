package com.forpets.domain.admin.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterAdminService {
    private final SitterProfileRepository sitterProfileRepository;
    private final MemberService memberService;
    private final SitterCacheService sitterCacheService;

//    @Cacheable(cacheNames = "adminPendingSitters", cacheManager = "shortTtlCacheManager")
    public List<AdminSitterResponseDto> getPendingSitters() {
        List<SitterProfile> pendingSitters = sitterProfileRepository
                .findAllByApprovalStatus(SitterApprovalStatus.PENDING);

        return pendingSitters.stream()
                .map(sitter -> {
                    Member member = memberService.findById(sitter.getMemberId());
                    return AdminSitterResponseDto.from(sitter, member.getRegion());
                })
                .toList();
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager"),
            @CacheEvict(cacheNames = "adminPendingSitters", allEntries = true, cacheManager = "shortTtlCacheManager")
    })
    public AdminSitterResponseDto approve(Long adminId, Long sitterId) {
        SitterProfile sitter = findById(sitterId);
        validatePendingApproval(sitter);

        sitter.approve(adminId);

        Member member = memberService.findById(sitter.getMemberId());
        member.changeRoleToSitter();

        // 시터 상세 캐시 무효화
        sitterCacheService.evictSitterDetail(sitterId);

        return AdminSitterResponseDto.from(sitter, member.getRegion());
    }


    @Transactional
    @CacheEvict(cacheNames = "adminPendingSitters", allEntries = true, cacheManager = "shortTtlCacheManager")//시터목록자체가 승인된 시터만 불러오기때문에 reject에서는 sitters쪽 캐시무효화는 불필요
    public AdminSitterResponseDto reject(Long adminId, Long sitterId, String rejectReason) {
        SitterProfile sitter = findById(sitterId);
        validatePendingApproval(sitter);

        sitter.reject(adminId, rejectReason);

        Member member = memberService.findById(sitter.getMemberId());

        return AdminSitterResponseDto.from(sitter, member.getRegion());
    }


    private SitterProfile findById(Long sitterId) {
        return sitterProfileRepository.findById(sitterId)
                .orElseThrow(() -> new SitterException(SitterErrorCode.SITTER_NOT_FOUND));
    }

    private void validatePendingApproval(SitterProfile sitter) {
        if (sitter.getApprovalStatus() != SitterApprovalStatus.PENDING) {
            throw new SitterException(SitterErrorCode.NOT_PENDING_APPROVAL);
        }
    }

}
