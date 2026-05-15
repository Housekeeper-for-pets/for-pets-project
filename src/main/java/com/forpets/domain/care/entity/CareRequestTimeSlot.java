package com.forpets.domain.care.entity;

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
public class CareRequestTimeSlot extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_request_id", nullable = false)
    private Long careRequestId;

    @Column(nullable = false)
    private LocalDate careDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int sequence;

    @Builder
    private CareRequestTimeSlot(Long careRequestId, LocalDate careDate,
                                LocalTime startTime, LocalTime endTime, int sequence) {
        this.careRequestId = careRequestId;
        this.careDate = careDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sequence = sequence;
    }
}
