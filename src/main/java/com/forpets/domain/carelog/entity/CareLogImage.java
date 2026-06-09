package com.forpets.domain.carelog.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "care_log_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareLogImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long careLogId;

    @Column(nullable = false)
    private String imageUrl;

    @Builder
    public CareLogImage(Long careLogId, String imageUrl) {
        this.careLogId = careLogId;
        this.imageUrl = imageUrl;
    }
}
