package com.forpets.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_time_slot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Embedded
    private TimeSlotInfo timeSlotInfo;

    public static PostTimeSlot create(Long postId, TimeSlotInfo timeSlotInfo) {
        PostTimeSlot slot = new PostTimeSlot();
        slot.postId = postId;
        slot.timeSlotInfo = timeSlotInfo;
        return slot;
    }
}
