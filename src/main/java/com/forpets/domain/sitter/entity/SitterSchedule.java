package com.forpets.domain.sitter.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "sitter_schedule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sitter_schedule_profile_day",
                columnNames = {"sitter_profile_id", "day_of_week"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SitterSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Builder
    private SitterSchedule(Long sitterProfileId, DayOfWeek dayOfWeek,
                           LocalTime startTime, LocalTime endTime) {
        this.sitterProfileId = sitterProfileId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
