package com.forpets.domain.admin.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.sitter.dto.admin.AdminSitterDetailResponseDto;
import com.forpets.domain.sitter.dto.admin.AdminSitterPageResponse;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterCacheService;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterAdminService {
    private final SitterProfileRepository sitterProfileRepository;
    private final MemberService memberService;
    private final SitterCacheService sitterCacheService;
    private final NotificationMessageBroker notificationBroker;

    /*
    승인 대기 시터 프로필 목록 (페이징)
    다른 목록 조회 API (Post, Sitter 등) 의 페이지네이션 컨벤션과 일치
    - 최신 신청 순 (createdAt DESC) 기본 정렬
     */
    public AdminSitterPageResponse getPendingSitters(int page, int size) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SitterProfile> pendingPage = sitterProfileRepository
                .findAllByApprovalStatus(SitterApprovalStatus.PENDING, pageable);

        var content = pendingPage.getContent().stream()
                .map(sitter -> {
                    Member member = memberService.findById(sitter.getMemberId());
                    return AdminSitterResponseDto.from(sitter, member.getRegion());
                })
                .toList();

        return AdminSitterPageResponse.of(
                content,
                pendingPage.getTotalElements(),
                pendingPage.getTotalPages(),
                pendingPage.getNumber(),
                pendingPage.getSize()
        );
    }

    /*
    시터 승인/거절 전, 시터 프로필 상세 + 신청자(Member) 정보 통합 조회.
    승인 대기 상태가 아니어도 (이미 승인/거절된 건도) 관리자는 조회 가능하게 둠.
     */
    public AdminSitterDetailResponseDto getSitterDetail(Long sitterProfileId) {
        SitterProfile sitter = findById(sitterProfileId);
        Member member = memberService.findById(sitter.getMemberId());
        return AdminSitterDetailResponseDto.from(sitter, member);
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

        notificationBroker.publish(NotificationEvent.of(
                sitter.getMemberId(),
                adminId,
                SseEventType.SITTER_PROFILE_APPROVED,
                "시터 프로필이 승인되었습니다.",
                sitter.getId(),
                "SITTER_PROFILE"
        ));

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

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new SitterException(SitterErrorCode.INVALID_PAGE_REQUEST);
        }
    }

}
