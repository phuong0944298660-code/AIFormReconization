package com.aiform.id995a.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrCheckboxDetectorTest {

  private final OcrCheckboxDetector detector = new OcrCheckboxDetector();

  @Test
  void detectsCheckedAndUncheckedBoxesAndBindsNearbyLabels() {
    BufferedImage image = new BufferedImage(240, 140, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setStroke(new BasicStroke(2));
    graphics.drawRect(20, 28, 20, 20);
    graphics.drawRect(20, 84, 20, 20);
    graphics.drawLine(25, 94, 30, 99);
    graphics.drawLine(30, 99, 38, 88);
    graphics.dispose();

    List<OcrTextBlock> blocks = List.of(
        new OcrTextBlock("text", "Unchecked option", List.of(52, 27, 180, 50), false),
        new OcrTextBlock("text", "Entry visa", List.of(52, 82, 140, 108), false)
    );

    List<OcrCheckbox> checkboxes = detector.detect(image, blocks);

    assertThat(checkboxes).hasSize(2);
    assertThat(checkboxes)
        .anySatisfy(checkbox -> {
          assertThat(checkbox.checked()).isFalse();
          assertThat(checkbox.label()).isEqualTo("Unchecked option");
        })
        .anySatisfy(checkbox -> {
          assertThat(checkbox.checked()).isTrue();
          assertThat(checkbox.label()).isEqualTo("Entry visa");
          assertThat(checkbox.confidence()).isGreaterThan(0.5);
        });
  }
}
