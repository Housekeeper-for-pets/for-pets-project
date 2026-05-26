package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.CreateSitterRequest;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.dto.profile.UpdateSitterRequest;
import com.forpets.domain.sitter.dto.profile.UpdateSitterStatusRequest;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.common.AssociationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterService {
    private final MemberService memberService;
    private final SitterProfileRepository sitterProfileRepository;
    private final SitterScheduleRepository sitterScheduleRepository;
    private final AssociationChecker associationChecker;
    private final SitterCacheService sitterCacheService;

    @Transactional
    @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager")
    public SitterResponseDto create(Long memberId, CreateSitterRequest request) {
        Member member = memberService.findById(memberId);
        validateNotAdmin(member);

        Optional<SitterProfile> sitterProfileOrNull = sitterProfileRepository.findByIdIncludingDeleted(memberId);

        if (sitterProfileOrNull.isPresent()) {
            SitterProfile sitter = sitterProfileOrNull.get();

            if (sitter.getApprovalStatus() == SitterApprovalStatus.REJECTED){
                throw new SitterException(SitterErrorCode.REQUEST_ALREADY_REJECTED);
            }

            if (sitter.isDeleted()){
                sitter.reactivate(); // 상태 초기화 해주기 PENDING 으로 + deleted 도 없애주고

                sitter.update(
                        request.introduction(),
                        request.experienceYears(),
                        request.possiblePetType(),
                        request.possiblePetSize(),
                        request.pricePerHour()
                );

                return SitterResponseDto.from(sitter, member.getRegion());
            }

            // deleted 가 풀렸고, 이미 승인 대기 중
            if (sitter.getApprovalStatus() == SitterApprovalStatus.PENDING){
                throw new SitterException(SitterErrorCode.SITTER_ALREADY_PENDING);
            }

            // 사용 가능한 시터 프로필이 존재함
            throw new SitterException(SitterErrorCode.SITTER_PROFILE_ALREADY_REGISTERED);
        }
//            validateProfileNotExists(memberId);

        SitterProfile sitter = SitterProfile.builder()
                .memberId(memberId)
                .introduction(request.introduction())
                .experienceYears(request.experienceYears())
                .possiblePetType(request.possiblePetType())
                .possiblePetSize(request.possiblePetSize())
                .pricePerHour(request.pricePerHour())
                .build();

        sitterProfileRepository.save(sitter);
        // 승인이 되어야 역할이 변경됨
//        member.changeRoleToSitter();

        return SitterResponseDto.from(sitter, member.getRegion());
    }

    /**
     * 시터 목록 검색 (GET /api/sitters)
     * - 정렬 필드 화이트리스트 검증 (createdAt / pricePerHour / experienceYears)
     * - 페이지 크기 최대 50 제한
     * - 페이지 번호 음수 불가
     * 캐시 무효화: 시터 프로필 수정(update), 상태 변경(updateStatus) 시 sitters:* 패턴 전체 무효화 필요
     */
//    @Cacheable(
//            cacheNames = "sitters",
//            keyGenerator = "sitterCacheKeyGenerator",
//            cacheManager = "longTtlCacheManager",
//            sync = true
//    )
//    public SitterPageResponse searchSitters(SitterSearchCondition condition, int page, int size, String sort) {
//        validatePageRequest(page, size);
//        validateSortField(sort);
//        validatePriceRange(condition);
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
//
//        return sitterProfileRepository.searchSitters(condition, pageable);
//    }

    public SitterPageResponse searchSitters(SitterSearchCondition condition, int page, int size, String sort) {
        validatePageRequest(page, size);
        validateSortField(sort);
        validatePriceRange(condition);

        return sitterCacheService.searchSitters(condition, page, size, sort);
    }

    public SitterResponseDto getSitterById(Long sitterId) {
        // 캐시 히트 시 DB 조회 0회, 미스 시 SitterCacheService 내부에서만 조회
        SitterResponseDto result = sitterCacheService.getSitterById(sitterId);
        if (result.approvalStatus() != SitterApprovalStatus.APPROVED) {
            throw new SitterException(SitterErrorCode.INVALID_SITTER_STATUS);
        }
        return result;
    }

    public SitterResponseDto getMyProfile(Long memberId) {
        SitterProfile sitter = findByMemberId(memberId);
        Member member = memberService.findById(memberId);

        // sitter profile 이랑 잘 출력 되는지 확인하고 싶어서 테스트 용으로 내 정보 조회에 넣어봤습니다!
        // 경안님도 아래 로직 똑같이 사용하면 getOne'sProfile API 구현할 때 편할 것 같아요
        List<SitterSchedule> schedules = sitterScheduleRepository.findAllBySitterProfileId(sitter.getId());

        return SitterResponseDto.from(sitter, member.getRegion(), schedules);
    }

    /*
    시터 프로필 수정 (전체 교체 방식)
     */
    @Transactional
    @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager")
    public SitterResponseDto update(Long memberId, UpdateSitterRequest request) {
        Member member = memberService.findById(memberId);
        SitterProfile sitter = findByMemberId(memberId);
        validateApproved(sitter);

        sitter.update(
                request.introduction(),
                request.experienceYears(),
                request.possiblePetType(),
                request.possiblePetSize(),
                request.pricePerHour()
        );

        sitterCacheService.evictSitterDetail(sitter.getId());
        return SitterResponseDto.from(sitter, member.getRegion());
    }

    /*
    시터 예약 가능 상태 변경 (RESERVABLE / NON_RESERVABLE)
    lock 을 걸어야 할 듯 합니다 상태를 변경하는 동안 예약이 들어올 수 있으니까!
     */
    @Transactional
    @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager")
    public SitterResponseDto updateStatus(Long memberId, UpdateSitterStatusRequest request) {
        Member member = memberService.findById(memberId);
        SitterProfile sitter = findByMemberId(memberId);
        validateApproved(sitter);

        sitter.changeStatus(request.status());

        sitterCacheService.evictSitterDetail(sitter.getId());
        return SitterResponseDto.from(sitter, member.getRegion());
    }

    /*
    시터 프로필 삭제 (Soft Delete)
    - 진행 중인 예약(PENDING/CONFIRMED)이 있으면 삭제 불가
    - 삭제 시 프로필 상태 DELETED + 회원 역할 MEMBER로 복원
     */
    @Transactional
    @CacheEvict(cacheNames = "sitters", allEntries = true, cacheManager = "longTtlCacheManager")
    public void delete(Long memberId) {
        SitterProfile sitter = findByMemberId(memberId);
        validateNotPending(sitter);
        if (associationChecker.hasSitterActiveAssociation(sitter.getId())){
            throw new SitterException(SitterErrorCode.SITTER_USED_IN_ACTIVE_PROCESS);
        }

        Long sitterId = sitter.getId();
        sitter.delete();

        Member member = memberService.findById(memberId);
        member.restoreRoleToMember();
        sitterCacheService.evictSitterDetail(sitterId);
    }

    // -------------Transaction 아닌 method 들------------------

    private void validateApproved(SitterProfile sitter) {
        if (!sitter.isApproved()) throw new SitterException(SitterErrorCode.INVALID_SITTER_STATUS);
    }

    // rejected 되면 삭제 후 재등록이 가능하도록 함
    private void validateNotPending(SitterProfile sitter){
        if (sitter.getApprovalStatus() == SitterApprovalStatus.PENDING){
            throw new SitterException(SitterErrorCode.SITTER_ALREADY_PENDING);
        }
    }

    /**
     * 정렬 필드 화이트리스트 검증
     * API 명세 허용 필드: createdAt(기본), pricePerHour, experienceYears
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "pricePerHour", "experienceYears"
    );

    private void validateSortField(String sort) {
        if (!ALLOWED_SORT_FIELDS.contains(sort)) {
            throw new SitterException(SitterErrorCode.INVALID_SORT_FIELD);
        }

    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new SitterException(SitterErrorCode.INVALID_PAGE_REQUEST);
        }
        if (size< 1 || size > 50) {
            throw new SitterException(SitterErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private void validatePriceRange(SitterSearchCondition condition) {
        if (condition.minPrice() != null && condition.maxPrice() != null
                && condition.minPrice() > condition.maxPrice()) {
            throw new SitterException(SitterErrorCode.INVALID_SEARCH_CONDITION);
        }
    }

    public SitterProfile findByMemberId(Long memberId){
        return sitterProfileRepository.findByMemberId(memberId).orElseThrow(
                ()->new SitterException(SitterErrorCode.SITTER_NOT_FOUND)
        );
    }

    public SitterProfile findById(Long sitterId){
        return sitterProfileRepository.findById(sitterId).orElseThrow(
                ()->new SitterException(SitterErrorCode.SITTER_NOT_FOUND)
        );
    }

    /*
    관리자는 관리 역할을 기본으로 하기 때문에 시터 프로필을 등록하는 것을 막아둠
    시터 프로필을 등록하려는 멤버가 관리자가 아닌지 확인
     */
    private void validateNotAdmin(Member member) {
        if (member.getRole() == MemberRole.ADMIN) {
            throw new SitterException(SitterErrorCode.ADMIN_CANNOT_REGISTER_SITTER);
        }
    }

    /*
    멤버는 하나의 시터 프로필만 생성할 수 있는 정책
    -> 이미 SitterProfile 을 생성한 Member 인지 확인
    존재한다면 throw

    MVP 에서는 시터 프로필을 삭제하게 되면 다시 시터 프로필을 생성할 수 없도록 설정
    V2 에서는 시터 프로필 삭제 후 다시 생성하면 review 등이 복구되는 조건으로 재생성 가능
     */
    private void validateProfileNotExists(Long memberId) {
        if (sitterProfileRepository.existsByMemberId(memberId)){
            throw new SitterException(SitterErrorCode.SITTER_PROFILE_EXISTS);
        }
        if (sitterProfileRepository.countByMemberIdIncludingDeleted(memberId) > 0) {
            throw new SitterException(SitterErrorCode.SITTER_PROFILE_ALREADY_REGISTERED);
        }
    }
}
