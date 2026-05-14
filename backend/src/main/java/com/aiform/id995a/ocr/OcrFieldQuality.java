package com.aiform.id995a.ocr;

import java.util.List;

public record OcrFieldQuality(
    String status,
    boolean accepted,
    boolean retryRecommended,
    List<String> reasons
) {

  public static OcrFieldQuality accept() {
    return new OcrFieldQuality("accepted", true, false, List.of());
  }

  public static OcrFieldQuality review(String reason) {
    return new OcrFieldQuality("needs_review", false, true, List.of(reason));
  }

  public static OcrFieldQuality rejected(String reason) {
    return new OcrFieldQuality("rejected_garbage", false, true, List.of(reason));
  }

  public OcrFieldQuality withReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return this;
    }
    java.util.ArrayList<String> merged = new java.util.ArrayList<>(reasons);
    merged.add(reason);
    return new OcrFieldQuality(status, accepted, retryRecommended, List.copyOf(merged));
  }
}
