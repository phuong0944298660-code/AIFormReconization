package com.aiform.id995a.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleOcrQualityClient implements OcrQualityLlmClient {

  private final boolean configuredEnabled;
  private final String baseUrl;
  private final String apiKey;
  private final String model;
  private final int maxTokens;
  private final Duration timeout;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiCompatibleOcrQualityClient(
      @Value("${llm.enabled:false}") boolean configuredEnabled,
      @Value("${llm.base-url:https://apie.zhisuaninfo.com/v1}") String baseUrl,
      @Value("${llm.api-key:}") String apiKey,
      @Value("${llm.model:Qwen3-Omni-30B-A3B-Instruct}") String model,
      @Value("${llm.max-tokens:256}") int maxTokens,
      @Value("${llm.timeout-seconds:20}") long timeoutSeconds,
      ObjectMapper objectMapper
  ) {
    this.configuredEnabled = configuredEnabled;
    this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    this.apiKey = apiKey == null ? "" : apiKey;
    this.model = model;
    this.maxTokens = maxTokens;
    this.timeout = Duration.ofSeconds(Math.max(3, timeoutSeconds));
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
  }

  @Override
  public boolean enabled() {
    return configuredEnabled && !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank();
  }

  @Override
  public OcrFieldQuality inspect(Id988aTemplateField field, String candidateText) {
    if (!enabled()) {
      return OcrFieldQuality.review("llm_quality_gate_disabled");
    }
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "model", model,
          "temperature", 0,
          "max_tokens", maxTokens,
          "messages", List.of(
              Map.of(
                  "role", "system",
                  "content",
                  "You are an OCR quality gate for immigration form fields. "
                      + "Only classify whether the candidate OCR text is usable. "
                      + "Do not correct, infer, rewrite, complete, or output any field value. "
                      + "Return compact JSON only: {\"verdict\":\"VALID|GARBLED|CROSSED_FIELD|REVIEW\",\"reasons\":[\"...\"]}."
              ),
              Map.of(
                  "role", "user",
                  "content",
                  "fieldKey=" + field.key()
                      + "\nfieldType=" + field.fieldType()
                      + "\nfieldLabel=" + field.label()
                      + "\ncandidateText=" + candidateText
                      + "\nClassify quality only. Never return corrected text."
              )
          )
      ));
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
          .timeout(timeout)
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return OcrFieldQuality.review("llm_quality_gate_http_" + response.statusCode());
      }
      return parseVerdict(response.body());
    } catch (Exception exception) {
      return OcrFieldQuality.review("llm_quality_gate_failed");
    }
  }

  private OcrFieldQuality parseVerdict(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      String content = root.path("choices").path(0).path("message").path("content").asText("");
      JsonNode verdictJson = objectMapper.readTree(extractJsonObject(content));
      String verdict = verdictJson.path("verdict").asText("REVIEW").toUpperCase(java.util.Locale.ROOT);
      List<String> reasons = new ArrayList<>();
      JsonNode reasonNodes = verdictJson.path("reasons");
      if (reasonNodes.isArray()) {
        for (JsonNode reasonNode : reasonNodes) {
          String reason = reasonNode.asText("");
          if (!reason.isBlank()) {
            reasons.add("llm_" + reason);
          }
        }
      }
      if ("VALID".equals(verdict)) {
        return OcrFieldQuality.accept();
      }
      String status = "CROSSED_FIELD".equals(verdict) || "GARBLED".equals(verdict)
          ? "rejected_garbage"
          : "needs_review";
      if (reasons.isEmpty()) {
        reasons.add("llm_" + verdict.toLowerCase(java.util.Locale.ROOT));
      }
      return new OcrFieldQuality(status, false, true, List.copyOf(reasons));
    } catch (Exception exception) {
      return OcrFieldQuality.review("llm_quality_gate_unparseable");
    }
  }

  private String extractJsonObject(String content) {
    int start = content.indexOf('{');
    int end = content.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return content.substring(start, end + 1);
    }
    return "{}";
  }
}
