package com.campushub.credit.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AppealStatusConverter implements AttributeConverter<AppealStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AppealStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AppealStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : AppealStatus.fromCode(dbData);
    }
}
