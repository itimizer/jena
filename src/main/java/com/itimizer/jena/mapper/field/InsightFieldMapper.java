package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Maps Insight (Assets) object fields, extracting the object key and label from the raw value. */
public class InsightFieldMapper implements BasicFieldMapper {

    @Override
    public String toValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        var regex = " \\(\\w+-\\d+\\)";
        var pattern = Pattern.compile(regex);
        final List<String> array = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (var node : value) {
                var matcher = pattern.matcher(node.asString());
                if (matcher.find()) {
                    array.add(matcher.group(0)
                            .replaceAll(" \\(", "")
                            .replaceAll("\\)", ""));
                } else {
                    return null;
                }

            }
            return "[" + String.join(", ", array) + "]";
        } else {
            return null;
        }
    }

    @Override
    public String toStringValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        var regex = " \\(\\w+-\\d+\\)";
        final List<String> array = new ArrayList<>(value.size());

        if (value.isArray()) {
            for (var node : value) {
                array.add(node.asString().replaceAll(regex, ""));
            }
        }
        return String.join(", ", array);
    }

    @Override
    public boolean isExernalProperties() {
        return false;
    }
}
