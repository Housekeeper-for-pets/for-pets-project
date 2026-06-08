package com.forpets.domain.ai.rag.client;

import com.forpets.domain.ai.rag.dto.RagDocument;
import com.forpets.domain.ai.rag.dto.RagSearchResultDto;

import java.util.List;

public interface RagVectorStore {

    void initializeCollection();

    void upsertReviews(List<RagDocument> documents, List<List<Double>> vectors);

    List<RagSearchResultDto> search(List<Double> queryVector, int limit, double scoreThreshold);
}
