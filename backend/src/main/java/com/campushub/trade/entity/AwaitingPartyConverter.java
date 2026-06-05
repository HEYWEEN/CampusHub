package com.campushub.trade.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AwaitingPartyConverter implements AttributeConverter<AwaitingParty, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AwaitingParty attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AwaitingParty convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : AwaitingParty.fromCode(dbData);
    }
}
