package com.forpets.domain.post.service;

import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.global.monitoring.TrackExecutionTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final PostRepository postRepository;

    @TrackExecutionTime("post.search")
    @Cacheable(
            cacheNames = "postings",
            keyGenerator = "postCacheKeyGenerator",
            cacheManager = "shortTtlCacheManager"
    )
    public PostPageResponse searchPostings(PostSearchCondition condition, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
        return postRepository.searchPosts(condition, pageable);
    }
}
