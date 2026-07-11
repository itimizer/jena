package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Maps multi user-picker fields to comma-joined user keys and display names. */
public class MultiUserFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        final List<String> users = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (final var node : value) {
                if (node.get("key") == null || node.get("key").isNull()) {
                    continue;
                }
                users.add(node.get("key").asString());
            }
        }
        return "[" + String.join(", ", users) + "]";
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        final List<String> users = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (final var node : value) {
                if (node.get("displayName") == null || node.get("displayName").isNull()) {
                    continue;
                }
                users.add(node.get("displayName").asString());
            }
        }
        return String.join(", ", users);
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
