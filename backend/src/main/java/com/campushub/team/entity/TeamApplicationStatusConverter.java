package com.campushub.team.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TeamApplicationStatusConverter implements AttributeConverter<TeamApplicationStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TeamApplicationStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TeamApplicationStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TeamApplicationStatus.fromCode(dbData);
    }
}
