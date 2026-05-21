package com.forpets.domain.post.repository;

import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    PostPageResponse searchPosts(PostSearchCondition condition, Pageable pageable);

    PostPageResponse searchMyPosts(Long memberId, PostStatus status, Pageable pageable);
}
