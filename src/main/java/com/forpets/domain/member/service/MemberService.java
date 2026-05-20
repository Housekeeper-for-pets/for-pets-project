package com.forpets.domain.member.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.member.dto.*;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.exception.MemberErrorCode;
import com.forpets.domain.member.exception.MemberException;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.global.security.jwt.BearerTokenResolver;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationRepository reservationRepository;
    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;
    private final SitterProfileRepository sitterProfileRepository;
    private final CareRequestRepository careRequestRepository;
    private final PetRepository petRepository;
    private final TokenRedisService tokenRedisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BearerTokenResolver bearerTokenResolver;

    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Transactional
    public UpdateMemberResponse updateMyInfo(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.getNickname().equals(request.nickname())
                && memberRepository.countByNicknameIncludingDeleted(request.nickname()) > 0) {
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
        }

        member.updateProfile(request.nickname(), request.phone(), request.gender(), request.region());
        return UpdateMemberResponse.from(member);
    }

    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.INVALID_PASSWORD);
        }

        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.SAME_PASSWORD);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void deleteAccount(Long memberId, HttpServletRequest request) {

        Member member = findById(memberId);

        // 진행 중 예약 체크
        if (reservationRepository.existsByGuardianIdAndStatusIn(
                memberId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))) {
            throw new MemberException(MemberErrorCode.HAS_ACTIVE_RESERVATION);
        }

        // 내 OPEN 공고 -> CLOSED + 해당 공고 PENDING 제안 -> REJECTED
        postRepository.findAllByMemberIdAndStatus(memberId, PostStatus.OPEN)
                .forEach(post -> {
                    proposalRepository.findAllByPostIdAndStatus(
                                    post.getId(), ProposalStatus.PENDING)
                            .forEach(Proposal::reject);
                    post.close();
                });

        // 내가 보낸 PENDING 제안 -> WITHDRAWN
        proposalRepository.findAllByMemberIdAndStatus(memberId, ProposalStatus.PENDING)
                .forEach(Proposal::withdraw);

        // 시터인 경우 받은 PENDING CareRequest -> CANCELED + SitterProfile Soft Delete
        sitterProfileRepository.findByMemberId(memberId)
                .ifPresent(sitter -> {
                    careRequestRepository.findAllBySitterProfileIdAndStatus(
                                    sitter.getId(), CareRequestStatus.PENDING)
                            .forEach(CareRequest::cancel);
                    sitter.delete();
                });

        // 연결된 Pet -> Soft Delete
        petRepository.findAllByMemberId(memberId)
                .forEach(Pet::delete);

        // 회원 탈퇴
        member.delete();

        // 토큰 무효화
        String accessToken = bearerTokenResolver.resolve(request);
        if (accessToken != null) {
            long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);
            tokenRedisService.addToBlacklist(accessToken, remainingTime);
        }
        tokenRedisService.deleteRefreshToken(memberId);
    }

    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
