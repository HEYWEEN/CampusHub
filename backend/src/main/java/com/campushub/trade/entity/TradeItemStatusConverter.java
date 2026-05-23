package com.campushub.trade.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TradeItemStatusConverter implements AttributeConverter<TradeItemStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TradeItemStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TradeItemStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TradeItemStatus.fromCode(dbData);
    }
}
