package com.aiform.id995a.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.aiform.id995a.template.Id995aTemplateRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PdfSignalExtractorSmokeTest {

  @Test
  void analyzesId995aPdfWithinDemoTimeout() throws Exception {
    Path samplePdf = Path.of("test-id995a.pdf");
    PdfSignalExtractor extractor = new PdfSignalExtractor(
        new Id995aTemplateRegistry(),
        new OptionalTess4jOcrService(false, "eng+chi_tra", ""),
        samplePdf.toString()
    );

    PdfAnalysis analysis = assertTimeoutPreemptively(Duration.ofSeconds(20), () ->
        extractor.analyze(Files.readAllBytes(samplePdf))
    );

    assertThat(analysis.pageSnapshots()).isNotEmpty();
    assertThat(analysis.extractedFields()).isNotEmpty();
    assertThat(analysis.engineStatus().extractionMode()).contains("PDFBox");
  }
}
