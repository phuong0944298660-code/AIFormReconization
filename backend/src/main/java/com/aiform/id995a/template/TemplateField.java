package com.aiform.id995a.template;

public record TemplateField(
    String key,
    String label,
    int page,
    double x,
    double y,
    double width,
    double height,
    double diffThreshold
) {}
