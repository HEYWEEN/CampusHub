package com.campushub.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Role attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Role convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : Role.fromCode(dbData);
    }
}
