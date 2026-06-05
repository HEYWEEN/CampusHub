package com.campushub.trade.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TradeOfferStatusConverter implements AttributeConverter<TradeOfferStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TradeOfferStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TradeOfferStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TradeOfferStatus.fromCode(dbData);
    }
}
