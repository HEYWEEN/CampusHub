package com.campushub.trade.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TradeOrderStatusConverter implements AttributeConverter<TradeOrderStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TradeOrderStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TradeOrderStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TradeOrderStatus.fromCode(dbData);
    }
}
