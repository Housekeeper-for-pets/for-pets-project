package com.forpets.domain.post.repository;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    List<Post> findAllByMemberIdAndStatus(Long memberId, PostStatus status);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Post p
        JOIN PostPet pp ON pp.postId = p.id
        WHERE pp.petId = :petId
        AND p.status IN :statuses
        """)
    boolean existsByPetIdAndStatusIn(@Param("petId") Long petId, @Param("statuses") List<PostStatus> statuses);

}