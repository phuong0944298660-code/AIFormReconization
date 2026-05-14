package com.aiform.id995a.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class TemplatePageAligner {

  private static final double TEMPLATE_WIDTH = 992.0;
  private static final double TEMPLATE_HEIGHT = 1403.0;

  public PageAlignment align(int pageNumber, OcrPage page) {
    BufferedImage image = decodeImage(page.sourceImageDataUrl());
    if (image == null) {
      return PageAlignment.identity(page.imageWidth(), page.imageHeight());
    }
    Anchor expected = expectedAnchor(pageNumber, image.getWidth(), image.getHeight());
    Anchor detected = detectAnchor(image);
    if (detected == null) {
      return PageAlignment.identity(image.getWidth(), image.getHeight());
    }
    return new PageAlignment(
        expected.left(),
        expected.top(),
        expected.right(),
        expected.bottom(),
        detected.left(),
        detected.top(),
        detected.right(),
        detected.bottom()
    );
  }

  private Anchor expectedAnchor(int pageNumber, int imageWidth, int imageHeight) {
    double scaleX = imageWidth / TEMPLATE_WIDTH;
    double scaleY = imageHeight / TEMPLATE_HEIGHT;
    return switch (pageNumber) {
      case 1 -> scaled(34, 348, 953, 1300, scaleX, scaleY);
      case 2 -> scaled(46, 66, 947, 1308, scaleX, scaleY);
      case 3 -> scaled(34, 66, 955, 1298, scaleX, scaleY);
      case 4 -> scaled(34, 66, 954, 1196, scaleX, scaleY);
      default -> new Anchor(0, 0, imageWidth, imageHeight);
    };
  }

  private Anchor scaled(double left, double top, double right, double bottom, double scaleX, double scaleY) {
    return new Anchor(left * scaleX, top * scaleY, right * scaleX, bottom * scaleY);
  }

  private Anchor detectAnchor(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    byte[] dark = darkMap(image);
    List<Integer> vertical = new ArrayList<>();
    for (int x = 0; x < width; x += 1) {
      int count = 0;
      for (int y = 0; y < height; y += 1) {
        count += dark[y * width + x];
      }
      if (count > height * 0.28) {
        vertical.add(x);
      }
    }
    List<Integer> horizontal = new ArrayList<>();
    for (int y = 0; y < height; y += 1) {
      int count = 0;
      for (int x = 0; x < width; x += 1) {
        count += dark[y * width + x];
      }
      if (count > width * 0.45) {
        horizontal.add(y);
      }
    }
    List<Span> xSpans = spans(vertical);
    List<Span> ySpans = spans(horizontal);
    if (xSpans.size() < 2 || ySpans.size() < 2) {
      return null;
    }
    Span left = xSpans.get(0);
    Span right = xSpans.get(xSpans.size() - 1);
    Span top = ySpans.get(0);
    Span bottom = ySpans.get(ySpans.size() - 1);
    if (right.center() - left.center() < width * 0.6 || bottom.center() - top.center() < height * 0.45) {
      return null;
    }
    return new Anchor(left.center(), top.center(), right.center(), bottom.center());
  }

  private List<Span> spans(List<Integer> values) {
    if (values.isEmpty()) {
      return List.of();
    }
    List<Span> spans = new ArrayList<>();
    int start = values.get(0);
    int previous = start;
    for (int index = 1; index < values.size(); index += 1) {
      int current = values.get(index);
      if (current > previous + 1) {
        spans.add(new Span(start, previous));
        start = current;
      }
      previous = current;
    }
    spans.add(new Span(start, previous));
    return spans;
  }

  private byte[] darkMap(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    byte[] dark = new byte[width * height];
    for (int y = 0; y < height; y += 1) {
      for (int x = 0; x < width; x += 1) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
        if (luminance < 90) {
          dark[y * width + x] = 1;
        }
      }
    }
    return dark;
  }

  private BufferedImage decodeImage(String imageDataUrl) {
    if (imageDataUrl == null || imageDataUrl.isBlank() || imageDataUrl.startsWith("http")) {
      return null;
    }
    String base64 = imageDataUrl;
    int comma = base64.indexOf(',');
    if (comma >= 0) {
      base64 = base64.substring(comma + 1);
    }
    try {
      return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    } catch (IllegalArgumentException | IOException exception) {
      return null;
    }
  }

  private record Anchor(double left, double top, double right, double bottom) {}

  private record Span(int start, int end) {
    double center() {
      return (start + end) / 2.0;
    }
  }
}
