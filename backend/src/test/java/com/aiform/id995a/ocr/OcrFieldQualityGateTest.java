package com.aiform.id995a.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OcrFieldQualityGateTest {

  @Test
  void asksLlmForBorderlineNoiseButDoesNotUseItToGenerateText() {
    OcrQualityLlmClient fakeLlm = new OcrQualityLlmClient() {
      @Override
      public boolean enabled() {
        return true;
      }

      @Override
      public OcrFieldQuality inspect(Id988aTemplateField field, String candidateText) {
        assertThat(candidateText).contains("E-mail address");
        return new OcrFieldQuality("rejected_garbage", false, true, java.util.List.of("llm_crossed_field"));
      }
    };
    OcrFieldQualityGate gate = new OcrFieldQualityGate(fakeLlm);
    Id988aTemplateField field = new Id988aTemplateField(
        "personalParticulars",
        "2. Personal Particulars",
        2,
        "contactTelephone.no.value",
        "联系电话 Contact telephone no.",
        "text_cells",
        "contactTelephone.no",
        "",
        "[0,0,1,1]",
        "[0,0,1,1]"
    );

    OcrFieldQuality quality = gate.assess(
        field,
        "+ 6 2 8/1 3 1.5之间 Contact telephone no. E-mail address Name of current employer",
        0.86
    );

    assertThat(quality.accepted()).isFalse();
    assertThat(quality.status()).isEqualTo("rejected_garbage");
    assertThat(quality.reasons()).contains("llm_crossed_field");
  }

  @Test
  void rejectsUnsupportedScriptsForId988aFieldValues() {
    OcrFieldQualityGate gate = OcrFieldQualityGate.localOnly();
    Id988aTemplateField field = new Id988aTemplateField(
        "personalParticulars",
        "2. Personal Particulars",
        2,
        "domicileAddress.value",
        "Domicile address",
        "multiline_text",
        "domicileAddress",
        "",
        "[0,0,1,1]",
        "[0,0,1,1]"
    );

    OcrFieldQuality quality = gate.assess(
        field,
        "JLAsiA SiRiA xN088 RTo4-RWog,Kelurahan GменCic have,Kecmananப்ப发声ede",
        0.86
    );

    assertThat(quality.accepted()).isFalse();
    assertThat(quality.status()).isEqualTo("rejected_garbage");
    assertThat(quality.reasons()).contains("unsupported_script_noise");
  }
}
