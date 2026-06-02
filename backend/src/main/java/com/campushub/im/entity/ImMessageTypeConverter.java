package com.campushub.im.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ImMessageTypeConverter implements AttributeConverter<ImMessageType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ImMessageType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ImMessageType convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : ImMessageType.fromCode(dbData);
    }
}
