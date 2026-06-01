package com.forpets.domain.ai.reviewsummary.service;

import com.forpets.domain.ai.reviewsummary.dto.ReviewSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("stub-review")
public class StubReviewSourceProvider implements ReviewSourceProvider {

    @Override
    public List<ReviewSource> findRecentReviewsBySitterId(Long sitterId, int limit) {
        return List.of(
                new ReviewSource(1001L, 5, "말티즈가 낯을 많이 가리는데 천천히 적응시켜주셔서 좋았어요. 사진도 자주 보내주셨습니다."),
                new ReviewSource(1002L, 5, "분리불안이 있는 아이인데 산책도 무리하지 않고 차분하게 케어해주셨어요."),
                new ReviewSource(1003L, 4, "응답이 빠르고 케어 일지도 자세했습니다. 대형견 후기는 많지 않아 보였어요.")
        ).stream().limit(limit).toList();
    }
}
