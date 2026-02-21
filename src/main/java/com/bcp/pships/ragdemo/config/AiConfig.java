package com.bcp.pships.ragdemo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    EmbeddingModel embeddingModel(
            @Value("${openai.api-key}") String key) {

        return OpenAiEmbeddingModel.builder()
                .apiKey(key)
                .modelName("text-embedding-3-small")
                .build();
    }

    @Bean
    ChatLanguageModel chatModel(
            @Value("${openai.api-key}") String key) {

        return OpenAiChatModel.builder()
                .apiKey(key)
                .modelName("gpt-4o-mini")
                .temperature(0.0)  // Más determinístico para RAG
                .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel,
            @Value("${pinecone.api-key}") String key) {

        return PineconeEmbeddingStore.builder()
                .apiKey(key)
                .index("docs-index")
                .environment("us-east-1")
                .build();
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)  // Solo documentos con alta relevancia (>70% similitud)
                .build();
    }
}