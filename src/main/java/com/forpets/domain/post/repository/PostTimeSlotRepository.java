package com.forpets.domain.post.repository;

import com.forpets.domain.post.entity.PostTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostTimeSlotRepository extends JpaRepository<PostTimeSlot, Long> {

    List<PostTimeSlot> findAllByPostIdOrderByTimeSlotInfoSequence(Long postId);

    List<PostTimeSlot> findAllByPostIdInOrderByTimeSlotInfoSequence(List<Long> postIds);

    void deleteAllByPostId(Long postId);
}
