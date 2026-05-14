package com.aiform.id995a.ocr;

public record Id988aTemplateField(
    String sectionKey,
    String sectionName,
    int page,
    String key,
    String label,
    String fieldType,
    String normalizedKey,
    String option,
    TemplateRect groupBox,
    TemplateRect valueBox
) {

  public Id988aTemplateField(
      String sectionKey,
      String sectionName,
      int page,
      String key,
      String label,
      String fieldType,
      String normalizedKey,
      String option,
      String groupBox,
      String valueBox
  ) {
    this(
        sectionKey,
        sectionName,
        page,
        key,
        label,
        fieldType,
        normalizedKey,
        option,
        TemplateRect.parse(groupBox),
        TemplateRect.parse(valueBox)
    );
  }
}
