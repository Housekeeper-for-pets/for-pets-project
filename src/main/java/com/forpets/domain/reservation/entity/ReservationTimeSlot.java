package com.forpets.domain.reservation.entity;

import com.forpets.global.entity.CreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "reservation_time_slot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationTimeSlot extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private LocalDate careDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int sequence;

    @Builder
    private ReservationTimeSlot(Long reservationId, LocalDate careDate,
                                LocalTime startTime, LocalTime endTime, int sequence) {
        this.reservationId = reservationId;
        this.careDate = careDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sequence = sequence;
    }
}
