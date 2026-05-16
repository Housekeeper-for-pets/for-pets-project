package com.forpets.domain.post.repository;

import com.forpets.domain.post.entity.PostPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPetRepository extends JpaRepository<PostPet, Long> {

    List<PostPet> findAllByPostId(Long postId);

    void deleteAllByPostId(Long postId);
}
