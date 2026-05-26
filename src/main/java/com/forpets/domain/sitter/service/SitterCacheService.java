package com.forpets.domain.sitter.service;

import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SitterCacheService {

    private final SitterProfileRepository sitterProfileRepository;

    @Cacheable(cacheNames = "sitters", keyGenerator = "sitterCacheKeyGenerator",
            cacheManager = "longTtlCacheManager")
    public SitterPageResponse searchSitters(SitterSearchCondition condition,
                                            int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
        return sitterProfileRepository.searchSitters(condition, pageable);
    }
}
