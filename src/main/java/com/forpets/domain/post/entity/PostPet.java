package com.forpets.domain.post.entity;

import com.forpets.global.entity.CreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_pet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPet extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Builder
    private PostPet(Long postId, Long petId) {
        this.postId = postId;
        this.petId = petId;
    }
}
