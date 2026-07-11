package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

/**
 * Maps single user-picker fields: {@code value} is the user key, {@code stringValue} the display
 * name.
 */
public class UserFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        if (value == null || value.get("key") == null || value.get("key").isNull()) {
            return null;
        }
        return value.get("key").asString();
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null || value.get("displayName") == null
                || value.get("displayName").isNull()) {
            return "";
        }
        return value.get("displayName").asString();
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
