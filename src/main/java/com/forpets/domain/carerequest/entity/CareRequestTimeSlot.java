package com.forpets.domain.carerequest.entity;

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
@Table(name = "care_request_time_slot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRequestTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_request_id", nullable = false)
    private Long careRequestId;

    @Embedded
    private TimeSlotInfo timeSlotInfo;

    public static CareRequestTimeSlot create(Long careRequestId, TimeSlotInfo timeSlotInfo) {
        CareRequestTimeSlot slot = new CareRequestTimeSlot();
        slot.careRequestId = careRequestId;
        slot.timeSlotInfo = timeSlotInfo;
        return slot;
    }
}
