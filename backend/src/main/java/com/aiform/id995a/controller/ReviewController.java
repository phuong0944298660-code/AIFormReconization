package com.aiform.id995a.controller;

import com.aiform.id995a.review.ReviewResponse;
import com.aiform.id995a.review.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReviewController {

  private final ReviewService reviewService;
  private final ObjectMapper objectMapper;
  private final Path rulesPath;

  public ReviewController(
      ReviewService reviewService,
      ObjectMapper objectMapper,
      @Value("${id995a.rules-path}") String rulesPath
  ) {
    this.reviewService = reviewService;
    this.objectMapper = objectMapper;
    this.rulesPath = Path.of(rulesPath).toAbsolutePath().normalize();
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "id995a-review-backend");
  }

  @GetMapping(value = "/rules/id995a", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Resource> rules() {
    if (!Files.exists(rulesPath)) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(new FileSystemResource(rulesPath));
  }

  @PostMapping(value = "/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ReviewResponse review(
      @RequestPart("file") MultipartFile file,
      @RequestPart(value = "metadata", required = false) String metadataJson
  ) throws IOException {
    Map<String, String> metadata = parseMetadata(metadataJson);
    return reviewService.review(file.getOriginalFilename(), file.getBytes(), metadata);
  }

  private Map<String, String> parseMetadata(String metadataJson) throws IOException {
    if (metadataJson == null || metadataJson.isBlank()) {
      return Map.of();
    }

    Map<String, Object> raw = objectMapper.readValue(metadataJson, new TypeReference<>() {});
    Map<String, String> normalized = new HashMap<>();
    raw.forEach((key, value) -> {
      if (value != null) {
        normalized.put(key, String.valueOf(value));
      }
    });
    return normalized;
  }
}
