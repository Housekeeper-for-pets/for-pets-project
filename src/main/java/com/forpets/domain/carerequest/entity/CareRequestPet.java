package com.forpets.domain.carerequest.entity;

import com.forpets.domain.pet.entity.Pet;
import com.forpets.global.embed.entity.PetSnapshot;
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
public class CareRequestPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "care_request_id", nullable = false)
    private Long careRequestId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Embedded
    private PetSnapshot petSnapshot;

    public static CareRequestPet createFrom(Long careRequestId, Pet pet) {
        CareRequestPet careRequestPet = new CareRequestPet();
        careRequestPet.careRequestId = careRequestId;
        careRequestPet.petId = pet.getId();
        careRequestPet.petSnapshot = PetSnapshot.from(pet);
        return careRequestPet;
    }
}