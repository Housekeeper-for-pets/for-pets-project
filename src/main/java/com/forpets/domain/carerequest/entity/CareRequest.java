package com.forpets.domain.carerequest.entity;

import com.forpets.global.common.CareType;
import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "care_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "sitter_profile_id", nullable = false)
    private Long sitterProfileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType careType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareRequestStatus status;

    @Builder
    private CareRequest(Long memberId, Long sitterProfileId,
                        CareType careType, String message) {
        this.memberId = memberId;
        this.sitterProfileId = sitterProfileId;
        this.careType = careType;
        this.message = message;
        this.status = CareRequestStatus.PENDING;
    }

    public void accept() {
        this.status = CareRequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = CareRequestStatus.REJECTED;
    }

    public void cancel() {
        this.status = CareRequestStatus.CANCELED;
    }

    public boolean isPending() {
        return this.status == CareRequestStatus.PENDING;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isTargetSitter(Long sitterProfileId) {
        return this.sitterProfileId.equals(sitterProfileId);
    }
}