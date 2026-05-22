package com.campushub.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class VerificationStatusConverter implements AttributeConverter<VerificationStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(VerificationStatus attribute) {
        return attribute == null ? VerificationStatus.PENDING.getCode() : attribute.getCode();
    }

    @Override
    public VerificationStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? VerificationStatus.PENDING : VerificationStatus.fromCode(dbData);
    }
}
