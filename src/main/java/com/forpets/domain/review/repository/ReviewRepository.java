package com.forpets.domain.review.repository;

import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByReservationId(Long reservationId);

    Page<Review> findAllByRevieweeIdAndDeletedFalse(Long revieweeId, Pageable pageable);

    @Query("""
            select r
            from Review r
            join Reservation reservation on r.reservationId = reservation.id
            where r.revieweeId = :revieweeId
              and r.deleted = false
              and reservation.status = :status
            """)
    Page<Review> findAllActiveByRevieweeIdAndReservationStatus(
            @Param("revieweeId") Long revieweeId,
            @Param("status") ReservationStatus status,
            Pageable pageable
    );
}
