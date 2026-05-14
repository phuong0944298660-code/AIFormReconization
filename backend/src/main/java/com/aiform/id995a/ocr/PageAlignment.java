package com.aiform.id995a.ocr;

public record PageAlignment(
    double templateLeft,
    double templateTop,
    double templateRight,
    double templateBottom,
    double imageLeft,
    double imageTop,
    double imageRight,
    double imageBottom
) {

  public ImageRect toImageRect(TemplateRect rect, int imageWidth, int imageHeight) {
    double templateX0 = rect.x() * imageWidth;
    double templateY0 = rect.y() * imageHeight;
    double templateX1 = (rect.x() + rect.width()) * imageWidth;
    double templateY1 = (rect.y() + rect.height()) * imageHeight;

    int x0 = clamp((int) Math.round(map(templateX0, templateLeft, templateRight, imageLeft, imageRight)), 0, imageWidth);
    int y0 = clamp((int) Math.round(map(templateY0, templateTop, templateBottom, imageTop, imageBottom)), 0, imageHeight);
    int x1 = clamp((int) Math.round(map(templateX1, templateLeft, templateRight, imageLeft, imageRight)), x0, imageWidth);
    int y1 = clamp((int) Math.round(map(templateY1, templateTop, templateBottom, imageTop, imageBottom)), y0, imageHeight);
    return new ImageRect(x0, y0, x1, y1);
  }

  public static PageAlignment identity(int imageWidth, int imageHeight) {
    return new PageAlignment(0, 0, imageWidth, imageHeight, 0, 0, imageWidth, imageHeight);
  }

  private double map(double value, double sourceMin, double sourceMax, double targetMin, double targetMax) {
    if (Math.abs(sourceMax - sourceMin) < 0.001) {
      return value;
    }
    return targetMin + (value - sourceMin) * (targetMax - targetMin) / (sourceMax - sourceMin);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
