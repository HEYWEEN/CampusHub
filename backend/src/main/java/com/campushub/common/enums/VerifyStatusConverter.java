package com.campushub.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter：VerifyStatus enum ↔ TINYINT 列。
 *
 * 使用方式（在实体字段上）：
 *   @Convert(converter = VerifyStatusConverter.class)
 *   private VerifyStatus verifyStatus;
 *
 * 不要用 @Enumerated(EnumType.ORDINAL) —— ordinal 在 enum 顺序变化时会静默错位。
 * 显式 code 列举更安全（P3 §3.1 数据库公约）。
 */
@Converter(autoApply = false)
public class VerifyStatusConverter implements AttributeConverter<VerifyStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VerifyStatus attribute) {
        return attribute == null ? VerifyStatus.GUEST.getCode() : attribute.getCode();
    }

    @Override
    public VerifyStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? VerifyStatus.GUEST : VerifyStatus.fromCode(dbData);
    }
}
