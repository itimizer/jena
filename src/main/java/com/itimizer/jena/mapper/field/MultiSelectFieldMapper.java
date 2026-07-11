package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Maps multi-option fields (multiselect/checkbox) to comma-joined ids and labels. */
public class MultiSelectFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        final List<String> options = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (final var node : value) {
                if (node.get("id") == null || node.get("id").isNull()) {
                    continue;
                }
                options.add(node.get("id").asString());
            }
        }
        return "[" + String.join(", ", options) + "]";
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        final List<String> options = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (final var node : value) {
                if (node.get("value") == null || node.get("value").isNull()) {
                    continue;
                }
                options.add(node.get("value").asString());
            }
        }
        return String.join(", ", options);
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
