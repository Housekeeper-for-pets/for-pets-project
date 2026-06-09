package com.forpets.domain.ai.rag.controller;

import com.forpets.domain.ai.rag.dto.RagIndexResponse;
import com.forpets.domain.ai.rag.dto.RagSearchRequest;
import com.forpets.domain.ai.rag.dto.RagSearchResponse;
import com.forpets.domain.ai.rag.service.AiRagService;
import com.forpets.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiRagController {

    private final AiRagService aiRagService;

    @PostMapping("/api/ai/rag/reviews/index")
    public ResponseEntity<ApiResponse<RagIndexResponse>> indexReviews() {
        return ResponseEntity.ok(ApiResponse.success(aiRagService.indexCompletedReviews()));
    }

    @PostMapping("/api/ai/rag/search")
    public ResponseEntity<ApiResponse<RagSearchResponse>> search(
            @Valid @RequestBody RagSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiRagService.search(request.query())));
    }
}
