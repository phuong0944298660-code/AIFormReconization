package com.aiform.id995a.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaddleOcrHttpClient implements PaddleOcrGateway {

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String baseUrl;
  private final Duration timeout;
  private final double topP;
  private final boolean visualize;

  public PaddleOcrHttpClient(
      ObjectMapper objectMapper,
      @Value("${ocr.paddle.base-url:http://192.168.20.250:18080}") String baseUrl,
      @Value("${ocr.paddle.timeout-seconds:180}") long timeoutSeconds,
      @Value("${ocr.paddle.top-p:1.0}") double topP,
      @Value("${ocr.paddle.visualize:true}") boolean visualize
  ) {
    this.objectMapper = objectMapper;
    this.baseUrl = stripTrailingSlash(baseUrl);
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.topP = topP;
    this.visualize = visualize;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  @Override
  public String analyze(byte[] fileBytes, int fileType) throws IOException {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("file", Base64.getEncoder().encodeToString(fileBytes));
    payload.put("fileType", fileType);
    payload.put("topP", topP);
    payload.put("visualize", visualize);

    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/layout-parsing"))
        .timeout(timeout)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(
          request,
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IOException("PaddleOCR-VL returned HTTP " + response.statusCode() + ": " + response.body());
      }
      return response.body();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("PaddleOCR-VL request interrupted", exception);
    }
  }

  private String stripTrailingSlash(String value) {
    String normalized = value == null || value.isBlank() ? "http://192.168.20.250:18080" : value.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
