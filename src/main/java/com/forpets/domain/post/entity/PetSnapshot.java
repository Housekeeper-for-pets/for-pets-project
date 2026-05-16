package com.forpets.domain.post.entity;

import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetSnapshot {

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetSpecies species;

    @Column(length = 50)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetSize size;

    @Column
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetGender gender;

    public static PetSnapshot from(Pet pet) {
        PetSnapshot snapshot = new PetSnapshot();
        snapshot.name = pet.getName();
        snapshot.species = pet.getSpecies();
        snapshot.breed = pet.getBreed();
        snapshot.size = pet.getSize();
        snapshot.age = pet.getAge();
        snapshot.gender = pet.getGender();
        return snapshot;
    }
}