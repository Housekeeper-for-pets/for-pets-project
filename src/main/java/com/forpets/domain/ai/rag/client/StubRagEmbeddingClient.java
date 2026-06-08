package com.forpets.domain.ai.rag.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@Profile("!ai")
public class StubRagEmbeddingClient implements RagEmbeddingClient {

    private static final int VECTOR_SIZE = 768;

    @Override
    public List<Double> embed(String text) {
        int hash = text == null ? 0 : text.hashCode();
        return IntStream.range(0, VECTOR_SIZE)
                .mapToObj(index -> ((hash + index * 31) % 1000) / 1000.0)
                .toList();
    }
}
