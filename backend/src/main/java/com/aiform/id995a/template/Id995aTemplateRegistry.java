package com.aiform.id995a.template;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class Id995aTemplateRegistry {

  private final List<TemplateField> fields = List.of(
      new TemplateField("surnameEn", "英文姓", 1, 0.080, 0.170, 0.360, 0.038, 0.010),
      new TemplateField("givenNamesEn", "英文名", 1, 0.080, 0.218, 0.360, 0.038, 0.010),
      new TemplateField("sex", "性别", 1, 0.505, 0.167, 0.120, 0.052, 0.004),
      new TemplateField("dateOfBirth", "出生日期", 1, 0.705, 0.170, 0.220, 0.038, 0.010),
      new TemplateField("placeOfBirth", "出生地点", 1, 0.505, 0.245, 0.420, 0.040, 0.010),
      new TemplateField("nationalityOrDomicile", "国籍/原居地", 1, 0.060, 0.780, 0.310, 0.060, 0.010),
      new TemplateField("travelDocumentType", "旅行证件类别", 1, 0.080, 0.345, 0.300, 0.038, 0.010),
      new TemplateField("travelDocumentNo", "旅行证件号码", 1, 0.505, 0.345, 0.360, 0.038, 0.010),
      new TemplateField("travelDocumentIssueDate", "旅行证件签发日期", 1, 0.505, 0.438, 0.240, 0.040, 0.010),
      new TemplateField("travelDocumentExpiryDate", "旅行证件届满日期", 1, 0.505, 0.500, 0.240, 0.040, 0.010),
      new TemplateField("mainlandIdentityNo", "内地身份证号码", 1, 0.555, 0.292, 0.360, 0.038, 0.010),
      new TemplateField("contactPhone", "联络电话", 1, 0.080, 0.575, 0.300, 0.038, 0.010),
      new TemplateField("presentAddress", "现时住址", 2, 0.075, 0.075, 0.670, 0.105, 0.010),
      new TemplateField("photo", "近照", 2, 0.760, 0.080, 0.165, 0.235, 0.018),
      new TemplateField("proposedEntryDate", "拟抵港日期", 2, 0.075, 0.390, 0.280, 0.040, 0.010),
      new TemplateField("proposedDuration", "拟在港逗留时间", 2, 0.395, 0.390, 0.280, 0.040, 0.010),
      new TemplateField("schoolNameAddress", "学校名称及地址", 3, 0.075, 0.085, 0.850, 0.085, 0.010),
      new TemplateField("course", "入读年级/课程", 3, 0.075, 0.205, 0.850, 0.060, 0.010),
      new TemplateField("livingCostTotal", "生活开支总计", 3, 0.350, 0.565, 0.260, 0.045, 0.010),
      new TemplateField("financialSupport", "经济状况", 3, 0.285, 0.675, 0.600, 0.130, 0.010),
      new TemplateField("previousShortTermStudyYes", "曾修短期课程-是", 3, 0.120, 0.845, 0.060, 0.040, 0.004),
      new TemplateField("previousShortTermStudyDetails", "短期课程详情", 3, 0.300, 0.820, 0.560, 0.095, 0.010),
      new TemplateField("declarationSignature", "声明签名", 4, 0.570, 0.905, 0.300, 0.050, 0.012),
      new TemplateField("declarationDate", "声明日期", 4, 0.115, 0.905, 0.210, 0.050, 0.010),
      new TemplateField("dependantPartB", "受养人乙部", 5, 0.070, 0.070, 0.860, 0.260, 0.010),
      new TemplateField("dependantName", "受养人姓名", 5, 0.080, 0.150, 0.400, 0.040, 0.010),
      new TemplateField("dependantSex", "受养人性别", 5, 0.510, 0.150, 0.150, 0.040, 0.004),
      new TemplateField("dependantDob", "受养人出生日期", 5, 0.080, 0.210, 0.250, 0.040, 0.010),
      new TemplateField("dependantNationality", "受养人国籍", 5, 0.380, 0.210, 0.250, 0.040, 0.010),
      new TemplateField("dependantTravelDocumentNo", "受养人旅行证件号码", 5, 0.080, 0.330, 0.400, 0.040, 0.010),
      new TemplateField("dependantSignature", "受养人/家长签名", 6, 0.490, 0.860, 0.380, 0.060, 0.012),
      new TemplateField("dependantSignatureDate", "受养人声明日期", 6, 0.080, 0.860, 0.300, 0.060, 0.010)
  );

  private final Map<String, TemplateField> byKey = fields.stream()
      .collect(Collectors.toUnmodifiableMap(TemplateField::key, Function.identity()));

  public List<TemplateField> fields() {
    return fields;
  }

  public Optional<TemplateField> find(String fieldKey) {
    return Optional.ofNullable(byKey.get(fieldKey));
  }
}
