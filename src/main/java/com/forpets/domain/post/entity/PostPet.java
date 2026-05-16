package com.forpets.domain.post.entity;

import com.forpets.domain.pet.entity.Pet;
import com.forpets.global.embed.entity.PetSnapshot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_pet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Embedded
    private PetSnapshot petSnapshot;

    public static PostPet createFrom(Long postId, Pet pet) {
        PostPet postPet = new PostPet();
        postPet.postId = postId;
        postPet.petId = pet.getId();
        postPet.petSnapshot = PetSnapshot.from(pet);
        return postPet;
    }
}