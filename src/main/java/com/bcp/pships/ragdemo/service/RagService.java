package com.bcp.pships.ragdemo.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ChatLanguageModel chatModel;
    private final ContentRetriever contentRetriever;

    public String answer(String question) {
        log.debug("RagService.answer called with question='{}'", question);

        List<Content> contents = contentRetriever.retrieve(Query.from(question));
        log.info("ContentRetriever returned {} items for question: '{}'", contents.size(), question);

        // Log retrieved chunks para debugging
        for (int i = 0; i < contents.size(); i++) {
            Content c = contents.get(i);
            String preview = c.textSegment().text().substring(0, Math.min(100, c.textSegment().text().length()));
            log.info("Retrieved chunk #{}: {}...", (i + 1), preview);
        }

        // Si no hay contenido relevante, responder directamente
        if (contents.isEmpty()) {
            log.warn("No relevant documents found for question: '{}'", question);
            return "No encontré información relevante en los documentos para responder tu pregunta.";
        }

        String context = contents.stream()
                .map(c -> "- " + c.textSegment().text() + (c.textSegment().metadata() != null
                        ? cite(c.textSegment().metadata())
                        : ""))
                .collect(Collectors.joining("\n"));

        log.debug("Built context with {} characters", context.length());

        String prompt = """
                Eres un asistente que responde preguntas EXCLUSIVAMENTE basándose en el CONTEXTO proporcionado.

                REGLAS ESTRICTAS:
                1. SOLO usa información que esté explícitamente en el CONTEXTO
                2. Si la respuesta está en el contexto, respóndela de forma clara y concisa
                3. Si NO está en el contexto, responde EXACTAMENTE: "No encontré información sobre esto en los documentos"
                4. NO uses tu conocimiento previo
                5. NO inventes ni asumas información que no esté en el contexto
                6. Si hay fuentes citadas [fuente: ...], menciónalas en tu respuesta
                7. Sé breve y directo

                CONTEXTO:
                %s

                PREGUNTA: %s

                RESPUESTA:""".formatted(context, question);

        log.debug("Invoking chat model for question: '{}'", question);
        String response = chatModel.generate(UserMessage.from(prompt)).content().text();
        log.info("Chat model returned response with {} characters", response != null ? response.length() : 0);
        return response;
    }

    private String cite(Metadata metadata) {
       String src = metadata.getString("source");
       return (src != null) ? " [fuente: " + src + "]" : "";
    }

}
