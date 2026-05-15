package com.forpets.domain.post.entity;

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
@Table(name = "post_time_slot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTimeSlot extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private LocalDate careDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int sequence;

    @Builder
    private PostTimeSlot(Long postId, LocalDate careDate, LocalTime startTime, LocalTime endTime, int sequence) {
        this.postId = postId;
        this.careDate = careDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sequence = sequence;
    }
}
