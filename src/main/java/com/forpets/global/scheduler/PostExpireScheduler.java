package com.forpets.global.scheduler;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.post.exception.PostException;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.service.PostExpireService;
import com.forpets.domain.post.service.PostLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/*
스케줄러: 마지막 sequence 의 timeslot 이 지난 OPEN 공고를 EXPIRED 로 만료 처리
기본 10분 간격 실행 (fixedDelay)

구조 (ReservationExpireScheduler 와 동일):
 1) 트랜잭션 바깥에서 OPEN 후보군 조회 - "미래 timeslot 이 하나도 없는" Post 만 잡힘
 2) for each list -> Post Lock 획득 (key: lock:post:{id})
       성공: PostExpireService.expireOne(id) 호출 (이 안에서 @Transactional + 상태 재검증)
       실패: skip (다른 변경 작업 진행 중 -> 다음 주기에 재시도)
 3) 건별 try/catch -> 한 건 실패해도 나머지 건은 계속 진행

PostExpireService 안에서 다음과 같이 처리:
 - post : OPEN -> EXPIRED
 - 해당 post 의 PENDING/ACCEPTED Proposal -> EXPIRED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostExpireScheduler {

    private final PostRepository postRepository;
    private final PostLockService postLockService;
    private final PostExpireService postExpireService;

    @Scheduled(fixedDelayString = "${post.expire.fixed-delay:600000}")
    public void expireOpenPosts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        List<Post> targets = postRepository.findExpireCandidates(PostStatus.OPEN, today, nowTime);

        if (targets.isEmpty()) {
            log.info("[PostExpireScheduler] 만료 대상 없음");
            return;
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Post post : targets) {
            Long postId = post.getId();
            try {
                postLockService.executeWithPostLock(postId, () -> {
                    postExpireService.expireOne(postId);
                    return null;
                });
                success++;
            } catch (PostException e) {
                if (e.getErrorCode() == PostErrorCode.POST_LOCK_FAILED) {
                    log.info("[PostExpireScheduler] Lock 획득 실패, 다음 주기 재시도 postId={}", postId);
                    skipped++;
                } else {
                    log.error("[PostExpireScheduler] 만료 처리 실패 postId={}", postId, e);
                    failed++;
                }
            } catch (Exception e) {
                log.error("[PostExpireScheduler] 만료 처리 실패 postId={}", postId, e);
                failed++;
            }
        }

        log.info("[PostExpireScheduler] 만료 처리 결과 대상={}, 성공={}, 스킵={}, 실패={}",
                targets.size(), success, skipped, failed);
    }
}
