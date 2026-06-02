package com.campushub.team.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TeamRecruitStatusConverter implements AttributeConverter<TeamRecruitStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TeamRecruitStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TeamRecruitStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TeamRecruitStatus.fromCode(dbData);
    }
}
