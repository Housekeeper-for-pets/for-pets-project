package com.forpets.domain.review.repository;

import com.forpets.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByReservationId(Long reservationId);

    Page<Review> findAllByRevieweeIdAndDeletedFalse(Long revieweeId, Pageable pageable);
}
