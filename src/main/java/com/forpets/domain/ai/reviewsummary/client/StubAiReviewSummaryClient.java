package com.forpets.domain.ai.reviewsummary.client;

import com.forpets.domain.ai.reviewsummary.dto.SitterReviewSummaryResponse;
import com.forpets.domain.ai.reviewsummary.entity.ReviewSentiment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"local", "postmantest", "test"})
public class StubAiReviewSummaryClient implements AiReviewSummaryClient {

    @Override
    public AiReviewSummaryResult generate(String prompt) {
        SitterReviewSummaryResponse response = new SitterReviewSummaryResponse(
                "소형견과 예민한 반려견을 차분하게 케어했다는 후기가 많습니다. 보호자들은 사진 공유와 빠른 응답을 장점으로 언급했습니다.",
                List.of("소형견 케어", "분리불안 반려견 대응", "꼼꼼한 사진 공유", "빠른 보호자 응답"),
                List.of("대형견 케어 후기는 아직 많지 않습니다."),
                List.of("말티즈", "포메라니안", "분리불안 반려견", "낯가림이 있는 반려견"),
                List.of("소형견", "분리불안", "산책", "사진공유", "차분한 케어"),
                ReviewSentiment.POSITIVE,
                0.87,
                3,
                List.of(1001L, 1002L, 1003L)
        );

        return new AiReviewSummaryResult(response, "stub-review-summary");
    }
}
