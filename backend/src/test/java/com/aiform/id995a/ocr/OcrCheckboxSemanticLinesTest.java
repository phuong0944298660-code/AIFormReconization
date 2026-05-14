package com.aiform.id995a.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OcrCheckboxSemanticLinesTest {

  @Test
  void insertsCheckedApplicationTypeMeaningAfterMatchingOcrLine() {
    List<OcrTextLine> lines = OcrTextHighlighter.toLines("""
        1. 申請類別 Application Type
        (a)來港受僱為外籍家庭傭工 Entry to Hong Kong to take up employment as a domestic helper from abroad 入境簽證 Country visa
        (b) 與同一僱主續約或轉換僱主 Contract renewal with the same employer or change of employer
        """);
    List<OcrCheckbox> checkboxes = List.of(
        new OcrCheckbox(1, true, 0.98, "", List.of(878, 518, 902, 542)),
        new OcrCheckbox(2, false, 0.74, "", List.of(878, 642, 902, 666))
    );
    List<OcrTextBlock> blocks = List.of(
        new OcrTextBlock(
            "table",
            """
            <table><tr><td colspan="5">(a)來港受僱為外籍家庭傭工 Entry to Hong Kong to take up employment as a domestic helper from abroad</td><td>入境簽證 Entry visa</td></tr>
            <tr><td>(b) 與同一僱主續約或轉換僱主 Contract renewal with the same employer or change of employer</td></tr></table>
            """,
            List.of(74, 441, 1129, 1529),
            false
        )
    );

    List<OcrTextLine> merged = OcrCheckboxSemanticLines.merge(lines, checkboxes, blocks, 1191, 1684);
    List<String> text = merged.stream()
        .map(line -> line.spans().stream().map(OcrTextSpan::text).reduce("", String::concat))
        .toList();

    assertThat(text).contains(
        "【来港受雇为外籍家庭雇工，入境签证，勾选框已勾选】"
    );
    assertThat(text.indexOf("【来港受雇为外籍家庭雇工，入境签证，勾选框已勾选】"))
        .isEqualTo(2);
  }

  @Test
  void infersEntryVisaCheckboxFromGeometryWhenOcrTableOmitsRightColumnLabel() {
    List<OcrTextLine> lines = OcrTextHighlighter.toLines("""
        1. 申请类别 Application Type
        (a) 来港受雇为外籍家庭雇工
        Entry to Hong Kong to take up employment as a domestic helper from abroad
        (b) 与同一雇主续约或转换雇主
        """);
    List<OcrCheckbox> checkboxes = List.of(
        new OcrCheckbox(1, true, 0.98, "", List.of(878, 518, 902, 542)),
        new OcrCheckbox(2, false, 0.74, "", List.of(878, 642, 902, 666))
    );
    List<OcrTextBlock> blocks = List.of(
        new OcrTextBlock(
            "table",
            """
            <table>
            <tr><td colspan="14">1. 申請類別 Application Type (請選擇下列申請類別 Please select type of application below)</td><td></td><td></td></tr>
            <tr><td colspan="14">(a) 來港受僱為外籍家庭傭工</td><td></td><td></td></tr>
            <tr><td colspan="14">entry to Hong Kong to take up employment as a domestic helper from abroad</td><td></td><td></td></tr>
            <tr><td colspan="14">(b) 與同一僱主續約或轉換僱主</td><td></td><td></td></tr>
            </table>
            """,
            List.of(74, 441, 1129, 1529),
            false
        )
    );

    List<OcrTextLine> merged = OcrCheckboxSemanticLines.merge(lines, checkboxes, blocks, 1191, 1684);
    List<String> text = merged.stream()
        .map(line -> line.spans().stream().map(OcrTextSpan::text).reduce("", String::concat))
        .toList();

    assertThat(text).contains(
        "【来港受雇为外籍家庭雇工，入境签证，勾选框已勾选】"
    );
    assertThat(text.indexOf("【来港受雇为外籍家庭雇工，入境签证，勾选框已勾选】"))
        .isEqualTo(3);
  }
}
