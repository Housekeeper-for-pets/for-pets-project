package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.CreateSitterRequest;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.UpdateSitterRequest;
import com.forpets.domain.sitter.dto.profile.UpdateSitterStatusRequest;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitterService {
    private final MemberService memberService;
    private final SitterProfileRepository sitterProfileRepository;

    @Transactional
    public SitterResponseDto create(Long memberId, CreateSitterRequest request) {
        Member member = memberService.findById(memberId);
        validateNotAdmin(member);
        validateProfileNotExists(memberId);

        SitterProfile sitter = SitterProfile.builder()
                .memberId(memberId)
                .region(request.region())
                .introduction(request.introduction())
                .experienceYears(request.experienceYears())
                .possiblePetType(request.possiblePetType())
                .possiblePetSize(request.possiblePetSize())
                .pricePerHour(request.pricePerHour())
                .build();

        sitterProfileRepository.save(sitter);
        member.changeRoleToSitter();

        return SitterResponseDto.from(sitter);
    }

    public SitterResponseDto getMyProfile(Long memberId) {
        SitterProfile sitter = findByMemberId(memberId);
        return SitterResponseDto.from(sitter);
    }

    /*
    시터 프로필 수정 (전체 교체 방식)
     */
    @Transactional
    public SitterResponseDto update(Long memberId, UpdateSitterRequest request) {
        SitterProfile sitter = findByMemberId(memberId);

        sitter.update(
                request.region(),
                request.introduction(),
                request.experienceYears(),
                request.possiblePetType(),
                request.possiblePetSize(),
                request.pricePerHour()
        );

        return SitterResponseDto.from(sitter);
    }

    /*
    시터 예약 가능 상태 변경 (RESERVABLE / NON_RESERVABLE)
    lock 을 걸어야 할 듯 합니다 상태를 변경하는 동안 예약이 들어올 수 있으니까!
     */
    @Transactional
    public SitterResponseDto updateStatus(Long memberId, UpdateSitterStatusRequest request) {
        SitterProfile sitter = findByMemberId(memberId);

        sitter.changeStatus(request.status());

        return SitterResponseDto.from(sitter);
    }

    /*
    시터 프로필 삭제 (Soft Delete)
    - 진행 중인 예약(PENDING/CONFIRMED)이 있으면 삭제 불가
    - 삭제 시 프로필 상태 DELETED + 회원 역할 MEMBER로 복원
     */
    @Transactional
    public void delete(Long memberId) {
        SitterProfile sitter = findByMemberId(memberId);
        validateNoActiveReservation(sitter.getId());

        sitter.delete();

        Member member = memberService.findById(memberId);
        member.restoreRoleToMember();
    }

    // -------------Transaction 아닌 method 들------------------

    public SitterProfile findByMemberId(Long memberId){
        return sitterProfileRepository.findByMemberId(memberId).orElseThrow(
                ()->new BusinessException(CommonErrorCode.SITTER_NOT_FOUND)
        );
    }

    /*
    관리자는 관리 역할을 기본으로 하기 때문에 시터 프로필을 등록하는 것을 막아둠
    시터 프로필을 등록하려는 멤버가 관리자가 아닌지 확인
     */
    private void validateNotAdmin(Member member) {
        if (member.getRole() == MemberRole.ADMIN) {
            throw new BusinessException(CommonErrorCode.ADMIN_CANNOT_REGISTER_SITTER);
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
            throw new BusinessException(CommonErrorCode.SITTER_PROFILE_EXISTS);
        }
        if (sitterProfileRepository.countByMemberIdIncludingDeleted(memberId) > 0) {
            throw new BusinessException(CommonErrorCode.SITTER_PROFILE_ALREADY_REGISTERED);
        }
    }

    /*
    시터에게 진행 중인 예약(PENDING/CONFIRMED)이 있는지 확인
     */
    private void validateNoActiveReservation(Long sitterId) {
        // if (reservationService.existsInProgressBySitterId(sitterId)) {
        //     throw new CustomException(ErrorCode.HAS_ACTIVE_RESERVATION);
        // }
    }
}
