package com.forpets.domain.pet.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetSpecies species;

    @Column(length = 50)
    private String breed; // 품종

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetSize size;

    @Column
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetGender gender;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    @Builder
    public Pet(Long memberId, String name, PetSpecies species, String breed,
               PetSize size, Integer age, PetGender gender,
               String profileImageUrl, String note) {
        this.memberId = memberId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.size = size;
        this.age = age;
        this.gender = (gender != null) ? gender : PetGender.UNKNOWN;
        this.profileImageUrl = profileImageUrl;
        this.note = note;
    }

    public void update(String name, PetSpecies species, String breed,
                       PetSize size, Integer age, PetGender gender,
                       String profileImageUrl, String note) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.size = size;
        this.age = age;
        this.gender = (gender != null) ? gender : PetGender.UNKNOWN;
        this.profileImageUrl = profileImageUrl;
        this.note = note;
    }
}