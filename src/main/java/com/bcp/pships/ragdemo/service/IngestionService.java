package com.bcp.pships.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public int ingestFolder(Path folder) {
        log.info("Starting ingestion for folder={}", folder);

        List<Document> docs = FileSystemDocumentLoader.loadDocuments(folder).stream()
                .filter(doc -> {
                    String name = doc.metadata().get("file_name");
                    return name != null && (name.endsWith(".txt") || name.endsWith(".md"));
                })
                .collect(Collectors.toList());

        log.info("Found {} documents to ingest", docs.size());

        Tokenizer tokenizer = new OpenAiTokenizer();
        DocumentSplitter splitter = new DocumentByParagraphSplitter(
                512,
                100,
                tokenizer);

        int count = 0;
        for (Document doc : docs) {
            List<TextSegment> segments = splitter.split(doc);
            log.debug("Document {} produced {} segments", doc.metadata().get("file_name"), segments.size());
            for (TextSegment segment : segments) {
                log.debug("Embedding segment (first 100 chars): {}", segment.text().length() > 100 ? segment.text().substring(0, 100) : segment.text());
                Embedding embedding = embeddingModel.embed(segment.text()).content();
                // Si embeddingStore tiene API pública para upsert, habría que usarla aquí. Por ahora solo se loggea.
                log.debug("Generated embedding vector length={}", embedding.vector().length);

                embeddingStore.add(embedding, segment);
                log.debug("Upserted segment id={} vectorLength={}", (count + 1), embedding.vector().length);

                count++;
            }
        }
        log.info("Completed ingestion. Total segments={}", count);
        return count;
    }
}
