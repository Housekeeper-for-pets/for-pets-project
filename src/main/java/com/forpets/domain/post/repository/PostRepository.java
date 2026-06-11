package com.forpets.domain.post.repository;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    List<Post> findAllByMemberIdAndStatus(Long memberId, PostStatus status);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Post p
        JOIN PostPet pp ON pp.postId = p.id
        WHERE pp.petId = :petId
        AND p.status IN :statuses
        """)
    boolean existsByPetIdAndStatusIn(@Param("petId") Long petId, @Param("statuses") List<PostStatus> statuses);

    /*
    마지막 sequence timeslot 이 지난 OPEN 공고 만료 후보 조회
    "미래 timeslot 이 하나도 없음" = 모든 timeslot 이 과거 = 마지막(가장 미래) timeslot 도 과거
    timeslot 이 하나라도 있는 공고만 대상 (방어적)
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.status = :status
          AND EXISTS (SELECT 1 FROM PostTimeSlot ts WHERE ts.postId = p.id)
          AND NOT EXISTS (
              SELECT 1 FROM PostTimeSlot ts
              WHERE ts.postId = p.id
                AND (ts.timeSlotInfo.careDate > :today
                     OR (ts.timeSlotInfo.careDate = :today AND ts.timeSlotInfo.endTime > :nowTime))
          )
        """)
    List<Post> findExpireCandidates(@Param("status") PostStatus status,
                                    @Param("today") LocalDate today,
                                    @Param("nowTime") LocalTime nowTime);
}