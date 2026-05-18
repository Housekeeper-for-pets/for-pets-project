package com.forpets.domain.member.service;

import com.forpets.domain.member.dto.*;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.exception.MemberErrorCode;
import com.forpets.domain.member.exception.MemberException;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReservationRepository reservationRepository;
    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;

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
    public void deleteAccount(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (reservationRepository.existsByMemberIdAndStatusIn(
                memberId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))) {
            throw new MemberException(MemberErrorCode.HAS_ACTIVE_RESERVATION);
        }

        postRepository.findAllByMemberIdAndStatus(memberId, PostStatus.OPEN)
                        .forEach(post -> {
                            proposalRepository.findAllByPostIdAndStatus(post.getId(), ProposalStatus.PENDING)
                                    .forEach(Proposal::reject);
                            post.close();
                        });

        proposalRepository.findAllByMemberIdAndStatus(memberId, ProposalStatus.PENDING)
                        .forEach(Proposal::withdraw);

        member.delete();
    }

    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
