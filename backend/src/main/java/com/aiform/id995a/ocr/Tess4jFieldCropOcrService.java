package com.aiform.id995a.ocr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Tess4jFieldCropOcrService implements FieldCropOcrService {

  private final boolean enabled;
  private final String language;
  private final String dataPath;

  public Tess4jFieldCropOcrService(
      @Value("${ocr.tesseract.enabled:false}") boolean enabled,
      @Value("${ocr.tesseract.language:eng+chi_sim+chi_tra}") String language,
      @Value("${ocr.tesseract.data-path:}") String dataPath
  ) {
    this.enabled = enabled;
    this.language = language;
    this.dataPath = dataPath;
  }

  @Override
  public CropOcrResult recognize(Id988aTemplateField field, BufferedImage pageImage, ImageRect valueRect) {
    if (!enabled || pageImage == null || valueRect.width() <= 0 || valueRect.height() <= 0) {
      return new CropOcrResult("", 0.0, false, "tess4j_crop_disabled");
    }
    try {
      ITesseract tesseract = new Tesseract();
      tesseract.setLanguage(language);
      if (dataPath != null && !dataPath.isBlank()) {
        tesseract.setDatapath(dataPath);
      }
      applyTesseractHints(tesseract, field);
      BufferedImage crop = preprocess(crop(pageImage, valueRect, 4));
      String text = tesseract.doOCR(crop).replaceAll("\\s+", " ").trim();
      return new CropOcrResult(text, text.isBlank() ? 0.0 : 0.62, true, "tess4j_crop");
    } catch (Throwable throwable) {
      return new CropOcrResult("", 0.0, true, "tess4j_crop_failed");
    }
  }

  private void applyTesseractHints(ITesseract tesseract, Id988aTemplateField field) {
    String key = field.normalizedKey();
    if (key.contains("date") || key.contains("period") || "contactTelephone.ext".equals(key)) {
      tesseract.setTessVariable("tessedit_char_whitelist", "0123456789/-. ");
      return;
    }
    if ("contactTelephone.no".equals(key) || "faxNo".equals(key)) {
      tesseract.setTessVariable("tessedit_char_whitelist", "+0123456789 ()-/");
      return;
    }
    if ("email".equals(key)) {
      tesseract.setTessVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._%+-@");
    }
  }

  private BufferedImage crop(BufferedImage image, ImageRect rect, int pad) {
    int x0 = Math.max(0, rect.x0() - pad);
    int y0 = Math.max(0, rect.y0() - pad);
    int x1 = Math.min(image.getWidth(), rect.x1() + pad);
    int y1 = Math.min(image.getHeight(), rect.y1() + pad);
    return image.getSubimage(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
  }

  private BufferedImage preprocess(BufferedImage source) {
    int scale = 3;
    BufferedImage scaled = new BufferedImage(source.getWidth() * scale, source.getHeight() * scale, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D graphics = scaled.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.drawImage(source, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
    graphics.dispose();

    BufferedImage binary = new BufferedImage(scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D binaryGraphics = binary.createGraphics();
    binaryGraphics.setColor(Color.WHITE);
    binaryGraphics.fillRect(0, 0, binary.getWidth(), binary.getHeight());
    binaryGraphics.drawImage(scaled, 0, 0, null);
    binaryGraphics.dispose();
    return binary;
  }
}
