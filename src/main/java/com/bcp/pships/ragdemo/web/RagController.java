package com.bcp.pships.ragdemo.web;

import com.bcp.pships.ragdemo.service.IngestionService;
import com.bcp.pships.ragdemo.service.RagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService;
    private final IngestionService ingestionService;

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody String question) {
        log.info("/api/ask called");
        log.debug("Ask payload: {}", question);
        String answer = ragService.answer(question);
        log.info("/api/ask completed");
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestBody String folder) {
        log.info("/api/ingest called");
        log.debug("Ingest payload: {}", folder);
        int count = ingestionService.ingestFolder(Path.of(folder));
        log.info("/api/ingest completed, ingested {} segments", count);
        return ResponseEntity.ok("Ingested " + count + " segments.");
    }

}
