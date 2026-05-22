package com.campushub.credit.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link CreditDirection} ↔ TINYINT code 映射（仿 auth.VerificationStatusConverter）。
 */
@Converter(autoApply = false)
public class CreditDirectionConverter implements AttributeConverter<CreditDirection, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CreditDirection attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public CreditDirection convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : CreditDirection.fromCode(dbData);
    }
}
