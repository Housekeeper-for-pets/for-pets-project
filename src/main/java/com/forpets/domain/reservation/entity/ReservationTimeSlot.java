package com.forpets.domain.reservation.entity;

import com.forpets.global.embed.entity.TimeSlotInfo;
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
public class ReservationTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Embedded
    private TimeSlotInfo timeSlotInfo;

    public static ReservationTimeSlot create(Long reservationId, TimeSlotInfo timeSlotInfo) {
        ReservationTimeSlot slot = new ReservationTimeSlot();
        slot.reservationId = reservationId;
        slot.timeSlotInfo = timeSlotInfo;
        return slot;
    }
}
