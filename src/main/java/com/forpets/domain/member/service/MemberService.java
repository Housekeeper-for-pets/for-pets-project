package com.forpets.domain.member.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.coupon.entity.UserCouponStatus;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.member.dto.*;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
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
import com.forpets.domain.sitter.service.SitterCacheEvictor;
import com.forpets.global.security.jwt.BearerTokenResolver;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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
    private final UserCouponRepository userCouponRepository;
    private final SitterCacheEvictor sitterCacheEvictor;

    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        int couponCount = (int) userCouponRepository.countByUserIdAndStatus(
                memberId,
                UserCouponStatus.ACTIVE
        );

        return MemberResponse.of(member, couponCount);
    }

    @Transactional
    public UpdateMemberResponse updateMyInfo(Long memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.getNickname().equals(request.nickname())
                && memberRepository.countByNicknameIncludingDeleted(request.nickname()) > 0) {
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATED);
        }

        // 시터 목록/상세 응답에 노출·필터링되는 멤버 필드. 변경 전 값을 보관해 두고,
        // 실제로 바뀐 경우에만 시터 캐시를 무효화한다. (updateProfile은 null을 UNKNOWN으로 정규화하므로
        // 정규화 이후 값(member.getXxx())과 비교해야 정확하다.)
        String oldNickname = member.getNickname();
        MemberGender oldGender = member.getGender();
        Region oldRegion = member.getRegion();

        member.updateProfile(request.nickname(), request.phone(), request.gender(), request.region());

        evictSitterCachesIfNeeded(memberId, oldNickname, oldGender, oldRegion, member);

        return UpdateMemberResponse.from(member);
    }

    /**
     * 회원이 시터이고, 시터 응답에 들어가는 멤버 필드(nickname/gender/region)가 실제로 변경된 경우에만
     * 시터 상세/목록 캐시를 무효화한다.
     * <ul>
     *   <li>phone만 변경되거나 시터 프로필이 없는 일반 회원이면 무효화하지 않는다(불필요한 전체 목록 wipe 방지).</li>
     *   <li>region/gender는 시터 목록 캐시의 키 구성요소이자 검색 필터라, 값 stale뿐 아니라 검색 결과
     *       정확성까지 어긋나므로 목록 캐시 전체 무효화가 필요하다.</li>
     * </ul>
     */
    private void evictSitterCachesIfNeeded(Long memberId, String oldNickname, MemberGender oldGender,
                                           Region oldRegion, Member member) {
        boolean sitterFieldChanged =
                !Objects.equals(oldNickname, member.getNickname())
                        || oldGender != member.getGender()
                        || oldRegion != member.getRegion();
        if (!sitterFieldChanged) {
            return;
        }

        sitterProfileRepository.findByMemberId(memberId).ifPresent(sitter -> {
            sitterCacheEvictor.evictSitterDetail(sitter.getId());
            sitterCacheEvictor.evictSitterList();
        });
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
