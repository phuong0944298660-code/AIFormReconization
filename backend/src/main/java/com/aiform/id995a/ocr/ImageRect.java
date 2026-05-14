package com.aiform.id995a.ocr;

import java.util.List;

public record ImageRect(int x0, int y0, int x1, int y1) {

  public static ImageRect fromBbox(List<Integer> bbox) {
    return new ImageRect(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
  }

  public int width() {
    return Math.max(0, x1 - x0);
  }

  public int height() {
    return Math.max(0, y1 - y0);
  }

  public int area() {
    return width() * height();
  }

  public boolean containsCenter(List<Integer> bbox, int padding) {
    double centerX = (bbox.get(0) + bbox.get(2)) / 2.0;
    double centerY = (bbox.get(1) + bbox.get(3)) / 2.0;
    return centerX >= x0 - padding
        && centerX <= x1 + padding
        && centerY >= y0 - padding
        && centerY <= y1 + padding;
  }

  public boolean containsCenter(ImageRect other, int padding) {
    double centerX = (other.x0 + other.x1) / 2.0;
    double centerY = (other.y0 + other.y1) / 2.0;
    return centerX >= x0 - padding
        && centerX <= x1 + padding
        && centerY >= y0 - padding
        && centerY <= y1 + padding;
  }

  public double overlapRatio(ImageRect other) {
    int intersection = intersectionArea(other);
    int area = Math.max(1, Math.min(area(), other.area()));
    return (double) intersection / area;
  }

  public double overlapRatioOfOther(ImageRect other) {
    return (double) intersectionArea(other) / Math.max(1, other.area());
  }

  public int intersectionArea(ImageRect other) {
    int left = Math.max(x0, other.x0);
    int top = Math.max(y0, other.y0);
    int right = Math.min(x1, other.x1);
    int bottom = Math.min(y1, other.y1);
    return Math.max(0, right - left) * Math.max(0, bottom - top);
  }

  public List<Integer> toBbox() {
    return List.of(x0, y0, x1, y1);
  }
}
