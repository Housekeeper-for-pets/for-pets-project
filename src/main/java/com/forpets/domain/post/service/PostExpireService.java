package com.forpets.domain.post.service;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
호출 흐름
 PostExpireScheduler
 -> PostLockService.executeWithPostLock(...)
 -> PostExpireService.expireOne(postId)   :여기서부터 Transactional

 ReservationExpireService 와 동일한 패턴:
 - Lock 은 트랜잭션 밖에서 잡음
 - 트랜잭션 내부에서 상태 재검증 -> Lock 획득 ~ 트랜잭션 시작 사이에 close/delete 가 들어왔을 수 있음
 - Post.OPEN 이 아니면 skip

 내부 흐름:
 1) post 재조회
 2) 상태 가드 - OPEN 이 아니면 skip
 3) post.expire()                : OPEN -> EXPIRED
 4) 해당 공고의 PENDING/ACCEPTED Proposal -> EXPIRED
    (사용자 의도: 만료된 공고의 제안은 더 이상 수락 불가)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostExpireService {

    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;

    @Transactional
    public void expireOne(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("[PostExpireService] post 가 존재하지 않음 postId={}", postId);
            return;
        }

        if (post.getStatus() != PostStatus.OPEN) {
            log.info("[PostExpireService] OPEN 이 아님 -> skip postId={}, status={}",
                    postId, post.getStatus());
            return;
        }

        post.expire();
        expireActiveProposals(postId);

        log.info("[PostExpireService] 만료 처리 완료 postId={}", postId);
    }

    private void expireActiveProposals(Long postId) {
        List<Proposal> active = proposalRepository.findAllByPostIdAndStatusIn(
                postId, List.of(ProposalStatus.PENDING, ProposalStatus.ACCEPTED));
        if (active.isEmpty()) {
            return;
        }
        active.forEach(Proposal::expire);
        log.info("[PostExpireService] 공고 만료에 따른 Proposal 만료 처리 postId={}, count={}",
                postId, active.size());
    }
}
