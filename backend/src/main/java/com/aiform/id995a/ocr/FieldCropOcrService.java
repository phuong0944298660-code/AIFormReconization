package com.aiform.id995a.ocr;

import java.awt.image.BufferedImage;

public interface FieldCropOcrService {

  CropOcrResult recognize(Id988aTemplateField field, BufferedImage pageImage, ImageRect valueRect);

  static FieldCropOcrService disabled() {
    return (field, pageImage, valueRect) -> new CropOcrResult("", 0.0, false, "disabled");
  }

  record CropOcrResult(String text, double confidence, boolean attempted, String source) {}
}
