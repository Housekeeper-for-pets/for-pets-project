package com.forpets.domain.post.repository;

import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    List<Post> findAllByMemberIdAndStatus(Long memberId, PostStatus status);
}