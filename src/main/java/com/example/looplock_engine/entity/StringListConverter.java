package com.example.looplock_engine.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JPA converter that serializes a List<String> to a pipe-delimited TEXT column.
 * Pipe (|) is used as it is not present in alphanumeric account IDs.
 */
@Converter(autoApply = false)
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(DELIMITER, list);
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.asList(value.split(DELIMITER_REGEX));
    }
}
