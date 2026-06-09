package com.forpets.domain.ai.rag.client;

import java.util.List;

public interface RagEmbeddingClient {

    List<Double> embed(String text);
}
