package com.aiform.id995a.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aiform.id995a.ocr.PaddleOcrGateway;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = {
    "rag.enabled=false",
    "llm.enabled=false"
})
@AutoConfigureMockMvc
@Import(OcrControllerTest.FakePaddleOcrConfig.class)
class OcrControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void uploadsDocumentAndReturnsSplitScreenOcrPayload() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "id995a.pdf",
        "application/pdf",
        "%PDF-1.7".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/ocr").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.filename", equalTo("id995a.pdf")))
        .andExpect(jsonPath("$.pageCount", equalTo(1)))
        .andExpect(jsonPath("$.pages[0].page", equalTo(1)))
        .andExpect(jsonPath("$.pages[0].sourceImageDataUrl", startsWith("data:image/jpeg;base64,/9j/demo-page")))
        .andExpect(jsonPath("$.pages[0].lines[0].spans[1].text", equalTo("CHAN")))
        .andExpect(jsonPath("$.pages[0].lines[0].spans[1].userInput", equalTo(true)))
        .andExpect(jsonPath("$.pages[0].blocks[0].label", equalTo("text")));
  }

  @TestConfiguration
  static class FakePaddleOcrConfig {
    @Bean
    @Primary
    PaddleOcrGateway fakePaddleOcrGateway() {
      return (fileBytes, fileType) -> """
          {
            "logId": "test-log",
            "result": {
              "layoutParsingResults": [
                {
                  "inputImage": "/9j/demo-page",
                  "markdown": {
                    "text": "Surname in English CHAN\\nDate of birth 20-04-2026"
                  },
                  "prunedResult": {
                    "parsing_res_list": [
                      {
                        "block_label": "text",
                        "block_content": "Surname in English CHAN",
                        "block_bbox": [10, 20, 200, 40]
                      }
                    ]
                  },
                  "outputImages": {}
                }
              ]
            }
          }
          """;
    }
  }
}
