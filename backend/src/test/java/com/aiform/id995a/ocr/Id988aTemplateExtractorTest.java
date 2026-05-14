package com.aiform.id995a.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Id988aTemplateExtractorTest {

  @Test
  void extractsCheckedApplicationTypeFromTemplateValueBox() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        1,
        "",
        992,
        1403,
        "ID 988A Entry to Hong Kong to take up employment as a domestic helper from abroad",
        List.of(),
        List.of(),
        List.of(new OcrCheckbox(1, true, 0.96, "Entry visa", List.of(735, 425, 755, 445)))
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("applicationType.entryFromAbroad.entryVisa.checked");
      assertThat(field.normalizedKey()).isEqualTo("applicationType");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("checked");
      assertThat(field.confidence()).isGreaterThan(0.9);
    });
  }

  @Test
  void extractsTextFromBlocksInsideTemplateValueBox() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        1,
        "",
        992,
        1403,
        "ID 988A Entry to Hong Kong to take up employment as a domestic helper from abroad",
        List.of(),
        List.of(new OcrTextBlock("text", "KUSUMA", List.of(190, 735, 350, 765), true)),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("surnameEn.value");
      assertThat(field.normalizedKey()).isEqualTo("surnameEn");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("KUSUMA");
    });
  }

  @Test
  void skipsOversizedTableBlockAndFallsBackToMarkdownCellValue() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    String markdown = """
        ID 988A domestic helper
        <table>
          <tr><td>姓名(英文)Surname in English</td><td colspan="4">KUSUMA</td></tr>
          <tr><td>名(英文)Given names in English</td><td colspan="4">DEWI ANGGRAINI</td></tr>
          <tr><td>婚前姓氏（如適用）Maiden surname (if applicable)</td><td>WIPBOW</td></tr>
        </table>
        """;
    OcrPage page = new OcrPage(
        1,
        "",
        1191,
        1684,
        markdown,
        List.of(),
        List.of(new OcrTextBlock("table", "whole page table text that must not become a field value", List.of(40, 420, 1140, 1500), true)),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("surnameEn.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("KUSUMA");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("givenNamesEn.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("DEWI ANGGRAINI");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("maidenSurname.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("WIPBOW");
    });
  }

  @Test
  void extractsTextValueFromSignatureFieldsWhenOcrReadsHandwriting() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        "ID 988A domestic helper",
        List.of(),
        List.of(new OcrTextBlock("text", "KUSUMA DEWI", List.of(650, 1266, 900, 1292), true)),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("page2Confirmation.signature.present");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("KUSUMA DEWI");
    });
  }

  @Test
  void normalizesEmailWhenOcrBlockAlsoContainsThePrintedLabel() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        "ID 988A domestic helper",
        List.of(),
        List.of(new OcrTextBlock(
            "text",
            "電郵地址 E-mail address dewi.kusuma@yahoo.co.id",
            List.of(30, 484, 910, 518),
            true
        )),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("email.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("dewi.kusuma@yahoo.co.id");
    });
  }

  @Test
  void extractsEmailFromMarkdownCellThatAlsoContainsQualifierText() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    String markdown = """
        ID 988A domestic helper
        <table>
          <tr><td colspan="6">E-mail address (if any) dewi.kusuma@yahoo.co.id</td></tr>
        </table>
        """;
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        markdown,
        List.of(),
        List.of(),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("email.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("dewi.kusuma@yahoo.co.id");
    });
  }

  @Test
  void rejectsGeneratedNoiseForPhoneFieldsInsteadOfKeepingGarbledText() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        "ID 988A domestic helper",
        List.of(),
        List.of(new OcrTextBlock(
            "text",
            "+ 6 2 8/1 3 1.5之间\\nContact telephone no. slap 8極We / Ex潦\\n"
                + "E-mail address dewi,kudiseases@yahoo.com/5日期它 Name of current employer "
                + "Gameworkえ、日本落叶雰囲気の facilitatingaty in more than the same employer extension of stay",
            List.of(190, 428, 380, 456),
            true
        )),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("contactTelephone.no.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isBlank();
      assertThat(field.confidence()).isLessThan(0.5);
    });
  }

  @Test
  void keepsValidPhoneFieldsAfterQualityGate() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        "ID 988A domestic helper",
        List.of(),
        List.of(new OcrTextBlock(
            "text",
            "+62 813 41567 8910",
            List.of(190, 428, 378, 456),
            true
        )),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("contactTelephone.no.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("+62 813 41567 8910");
      assertThat(field.qualityStatus()).isEqualTo("accepted");
    });
  }

  @Test
  void rejectsNonDateNoiseForDateFields() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        1,
        "",
        992,
        1403,
        "ID 988A domestic helper",
        List.of(),
        List.of(new OcrTextBlock("text", "外交日加", List.of(450, 930, 610, 958), true)),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("dateOfBirth.value");
      assertThat(field.present()).isFalse();
      assertThat(field.value()).isBlank();
    });
  }

  @Test
  void extractsWorkingExperienceRowsFromMarkdownTableWithoutColumnCompression() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    String markdown = """
        ID 988A domestic helper
        <table>
          <tr><td>Name of employer(s)</td><td>Address</td><td>From (mm/yy)</td><td>To (mm/yy)</td></tr>
          <tr><td>Mrs. Aisha AL-KHALI 7A</td><td>Villa No 7, Street 211, Dubai, UAE</td><td>03/17</td><td>02/21</td></tr>
          <tr><td>Mr. Mohammad RASHID</td><td>Building No 7, Abu Dhabi, UAE</td><td>09/21</td><td>03/26</td></tr>
        </table>
        """;
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        markdown,
        List.of(),
        List.of(new OcrTextBlock("table", "whole working experience table", List.of(50, 730, 950, 1080), true)),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).noneSatisfy(field ->
        assertThat(field.key()).isEqualTo("workingExperience.items[].periodFrom.value")
    );
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].employerName.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("Mrs. Aisha AL-KHALI 7A");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].address.value");
      assertThat(field.value()).isEqualTo("Villa No 7, Street 211, Dubai, UAE");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].periodFrom.value");
      assertThat(field.value()).isEqualTo("03/17");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].periodTo.value");
      assertThat(field.value()).isEqualTo("02/21");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[1].employerName.value");
      assertThat(field.value()).isEqualTo("Mr. Mohammad RASHID");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[1].address.value");
      assertThat(field.value()).isEqualTo("Building No 7, Abu Dhabi, UAE");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[1].periodFrom.value");
      assertThat(field.value()).isEqualTo("09/21");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[1].periodTo.value");
      assertThat(field.value()).isEqualTo("03/26");
    });

    Id988aFieldExtraction row0Employer = fields.stream()
        .filter(field -> field.key().equals("workingExperience.items[0].employerName.value"))
        .findFirst()
        .orElseThrow();
    Id988aFieldExtraction row0From = fields.stream()
        .filter(field -> field.key().equals("workingExperience.items[0].periodFrom.value"))
        .findFirst()
        .orElseThrow();
    assertThat(row0Employer.groupBox()).isEqualTo(row0From.groupBox());
    assertThat(row0From.valueBox().x()).isGreaterThan(row0Employer.valueBox().x());
  }

  @Test
  void keepsWorkingExperienceRowsAsVisualItemsWhenMarkdownTableIsGarbage() throws Exception {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    BufferedImage image = new BufferedImage(992, 1403, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setStroke(new java.awt.BasicStroke(5));
    graphics.drawLine(65, 860, 290, 865);
    graphics.drawLine(310, 860, 650, 865);
    graphics.drawLine(670, 860, 760, 865);
    graphics.drawLine(810, 860, 900, 865);
    graphics.drawLine(65, 980, 290, 985);
    graphics.drawLine(310, 980, 650, 985);
    graphics.drawLine(670, 980, 760, 985);
    graphics.drawLine(810, 980, 900, 985);
    graphics.dispose();

    OcrPage page = new OcrPage(
        2,
        dataUrl(image),
        992,
        1403,
        """
            ID 988A domestic helper
            <table>
              <tr><td>Name of employer(s)</td><td>Address</td><td>From (mm/yy)</td><td>To (mm/yy)</td></tr>
              <tr><td>Mrs. Aisha AL-KHALI FAMr.Mohammad RASHID</td><td>Villa No.7, Street Z11, AL Wasl, Dubai, UAEBuilding No.7, AL Maryah Island, Abu Dhabi, UAE</td><td>03/11/2104/2103/26</td><td></td></tr>
            </table>
            """,
        List.of(),
        List.of(),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].employerName.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isBlank();
      assertThat(field.qualityStatus()).isEqualTo("needs_ocr_text");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[1].periodTo.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isBlank();
      assertThat(field.qualityStatus()).isEqualTo("needs_ocr_text");
    });
  }

  @Test
  void extractsWorkingExperienceFromRowspanTableWithoutHeaderOrFooterNoise() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    String markdown = """
        ID 988A domestic helper
        <table>
          <tr><td rowspan="2">僱主名稱Name of employer(s)</td><td rowspan="2">地址Address</td><td colspan="2">任職日期Period of employment</td></tr>
          <tr><td>由 （月/年）From (mm/yy)</td><td>至 （月/年）To (mm/yy)</td></tr>
          <tr><td>Mrs. Aisha Al-KHALI KAMr. Mohammed RASHID</td><td>Villa No 27, street 102, ALI - ALI Wasl, 03/27Dubai, UAEBuilding No 7, Al Maryah Island, Abu Dhabi, UAE</td><td colspan="2">04/2103/26</td></tr>
          <tr><td colspan="2">任職家庭僱工的工作經驗年資總計Total duration of working experience as a domestic helper</td><td colspan="2">8 年Year(s) 8 月Month(s)</td></tr>
          <tr><td>如本表為影印本或從互聯網下載</td><td>XC21|F2|b</td><td colspan="2">The information given on this page is correct, complete and true.</td></tr>
        </table>
        """;
    OcrPage page = new OcrPage(
        2,
        "",
        992,
        1403,
        markdown,
        List.of(),
        List.of(),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].employerName.value");
      assertThat(field.value()).isEqualTo("Mrs. Aisha Al-KHALI KAMr. Mohammed RASHID");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].address.value");
      assertThat(field.value()).isEqualTo("Villa No 27, street 102, ALI - ALI Wasl, 03/27Dubai, UAEBuilding No 7, Al Maryah Island, Abu Dhabi, UAE");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].periodFrom.value");
      assertThat(field.value()).isEqualTo("04/21");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.items[0].periodTo.value");
      assertThat(field.value()).isEqualTo("03/26");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.totalYears.value");
      assertThat(field.value()).isEqualTo("8");
    });
    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("workingExperience.totalMonths.value");
      assertThat(field.value()).isEqualTo("8");
    });
  }

  @Test
  void extractsUndertakingContractNumberFromMarkdownLabel() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    String markdown = """
        ID 988A domestic helper
        <table>
          <tr><td>D.H. Contract No.</td><td>AB123456</td></tr>
        </table>
        """;
    OcrPage page = new OcrPage(
        4,
        "",
        992,
        1403,
        markdown,
        List.of(),
        List.of(),
        List.of()
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).anySatisfy(field -> {
      assertThat(field.key()).isEqualTo("undertaking.contractNo.value");
      assertThat(field.present()).isTrue();
      assertThat(field.value()).isEqualTo("AB123456");
    });
  }

  @Test
  void skipsExtractionWhenDocumentDoesNotLookLikeId988a() {
    Id988aTemplateExtractor extractor = new Id988aTemplateExtractor(
        new Id988aTemplateRegistry(),
        new TemplatePageAligner()
    );
    OcrPage page = new OcrPage(
        1,
        "",
        992,
        1403,
        "Application for Entry for Study in Hong Kong",
        List.of(),
        List.of(new OcrTextBlock("text", "Application for Entry for Study in Hong Kong", List.of(30, 25, 500, 120), false)),
        List.of(new OcrCheckbox(1, true, 0.96, "Entry visa", List.of(735, 425, 755, 445)))
    );

    List<Id988aFieldExtraction> fields = extractor.extract(List.of(page));

    assertThat(fields).isEmpty();
  }

  private String dataUrl(BufferedImage image) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
  }
}
