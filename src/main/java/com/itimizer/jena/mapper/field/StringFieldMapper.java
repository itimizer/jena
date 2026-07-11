package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

/** Maps plain text/number custom fields: {@code stringValue} is the text, {@code value} is null. */
public class StringFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        return null;
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return "";
        }
        if (value.isNull()) {
            return "null";
        }
        if (value.isValueNode()) {
            return value.asString();
        }
        return "";
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
