package com.forpets.global.embed.dto;

import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.global.embed.entity.PetSnapshot;
import com.forpets.domain.post.entity.PostPet;

public record PetSnapshotResponseDto(
        Long petId,
        String name,
        PetSpecies species,
        String breed,
        PetSize size,
        Integer age,
        PetGender gender
) {
    public static PetSnapshotResponseDto from(PostPet postPet) {
        PetSnapshot s = postPet.getPetSnapshot();
        return new PetSnapshotResponseDto(
                postPet.getPetId(),
                s.getName(),
                s.getSpecies(),
                s.getBreed(),
                s.getSize(),
                s.getAge(),
                s.getGender()
        );
    }
}