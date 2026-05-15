package com.forpets.domain.sitter.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "sitter_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SitterProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    private int experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PossiblePetType possiblePetType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PossiblePetSize possiblePetSize;

    @Column(nullable = false)
    private int pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SitterProfileStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private SitterProfile(Long memberId, String region, String introduction,
                          int experienceYears, PossiblePetType possiblePetType,
                          PossiblePetSize possiblePetSize, int pricePerHour) {
        this.memberId = memberId;
        this.region = region;
        this.introduction = introduction;
        this.experienceYears = experienceYears;
        this.possiblePetType = possiblePetType;
        this.possiblePetSize = possiblePetSize == null ? PossiblePetSize.ALL : possiblePetSize;
        this.pricePerHour = pricePerHour;
        this.status = SitterProfileStatus.RESERVABLE;
    }

    public void update(String region, String introduction, int experienceYears,
                       PossiblePetType possiblePetType, PossiblePetSize possiblePetSize, int pricePerHour) {
        this.region = region;
        this.introduction = introduction;
        this.experienceYears = experienceYears;
        this.possiblePetType = possiblePetType;
        this.possiblePetSize = possiblePetSize;
        this.pricePerHour = pricePerHour;
    }

    public void markNonReservable() {
        this.status = SitterProfileStatus.NON_RESERVABLE;
    }

    public void markReservable() {
        this.status = SitterProfileStatus.RESERVABLE;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
