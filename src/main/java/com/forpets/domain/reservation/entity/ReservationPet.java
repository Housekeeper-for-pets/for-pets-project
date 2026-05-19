package com.forpets.domain.reservation.entity;

import com.forpets.global.embed.entity.PetSnapshot;
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
public class ReservationPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Embedded
    private PetSnapshot petSnapshot;

    public static ReservationPet createFrom(Long reservationId, Long petId, PetSnapshot snapshot) {
        ReservationPet rp = new ReservationPet();
        rp.reservationId = reservationId;
        rp.petId = petId;
        rp.petSnapshot = snapshot;
        return rp;
    }
}
