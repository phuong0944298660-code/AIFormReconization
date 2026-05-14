package com.aiform.id995a.ocr;

public interface OcrQualityLlmClient {

  boolean enabled();

  OcrFieldQuality inspect(Id988aTemplateField field, String candidateText);

  static OcrQualityLlmClient disabled() {
    return new OcrQualityLlmClient() {
      @Override
      public boolean enabled() {
        return false;
      }

      @Override
      public OcrFieldQuality inspect(Id988aTemplateField field, String candidateText) {
        return OcrFieldQuality.review("llm_quality_gate_disabled");
      }
    };
  }
}
