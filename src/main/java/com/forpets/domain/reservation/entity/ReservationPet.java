package com.forpets.domain.reservation.entity;

import com.forpets.global.entity.CreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reservation_pet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationPet extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Builder
    private ReservationPet(Long reservationId, Long petId) {
        this.reservationId = reservationId;
        this.petId = petId;
    }
}
