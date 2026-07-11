package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

/**
 * Maps single-option fields (select/radio): {@code value} is the option id, {@code stringValue} its
 * label.
 */
public class OptionFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        if (value == null || value.get("id") == null || value.get("id").isNull()) {
            return null;
        }
        return value.get("id").asString();
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null || value.get("value") == null || value.get("value").isNull()) {
            return "";
        }
        return value.get("value").asString();
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
