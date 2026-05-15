package com.forpets.domain.care.entity;

import com.forpets.global.entity.CreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "care_request_pet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRequestPet extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_request_id", nullable = false)
    private Long careRequestId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Builder
    private CareRequestPet(Long careRequestId, Long petId) {
        this.careRequestId = careRequestId;
        this.petId = petId;
    }
}
