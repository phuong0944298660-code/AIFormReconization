package com.aiform.id995a.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "rag.enabled=false",
    "llm.enabled=false",
    "id995a.template-path=test-id995a.pdf"
})
@AutoConfigureMockMvc
class ReviewControllerSmokeTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void uploadsId995aPdfAndReturnsReviewJsonWithinDemoTimeout() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test-id995a.pdf",
        "application/pdf",
        Files.readAllBytes(Path.of("test-id995a.pdf"))
    );

    assertTimeoutPreemptively(Duration.ofSeconds(25), () ->
        mockMvc.perform(multipart("/api/review")
                .file(file)
                .param("metadata", "{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verdict", notNullValue()))
            .andExpect(jsonPath("$.engineStatus.extractionMode", notNullValue()))
    );
  }
}
