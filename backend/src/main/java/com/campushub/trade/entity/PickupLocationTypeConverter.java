package com.campushub.trade.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PickupLocationTypeConverter implements AttributeConverter<PickupLocationType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PickupLocationType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public PickupLocationType convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : PickupLocationType.fromCode(dbData);
    }
}
