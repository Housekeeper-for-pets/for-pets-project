package com.forpets.domain.carelog.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "care_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long sitterMemberId;

    @Column(nullable = false, length = 1000)
    private String content;

    // imageUrls 필드 없음! CareLogImage 테이블에서 careLogId로 조회

    @Builder
    public CareLog(Long reservationId, Long sitterMemberId, String content) {
        this.reservationId = reservationId;
        this.sitterMemberId = sitterMemberId;
        this.content = content;
    }
}