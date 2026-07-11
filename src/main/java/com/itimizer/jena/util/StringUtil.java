package com.itimizer.jena.util;

import com.itimizer.jena.domain.TemplateField;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * String helpers: truncating error bodies for logs, stripping HTML/Jira markup from field values
 * before sending, and extracting a host URL.
 */
@Component
public class StringUtil {

    public static final int MAX_ERROR_BODY_LENGTH = 2000;

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength)
                + "... (truncated " + (value.length() - maxLength) + " chars)";
    }

    public String removeHtmlTags(String htmlString) {
        if (htmlString == null) {
            return "";
        }
        return HTML_TAG.matcher(htmlString).replaceAll("");
    }

    public String getBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        try {
            return new URI(url).resolve("/").toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String getHostUrl(String url) {
        if (url == null) {
            return "";
        }
        try {
            return new URI(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    public String removeFieldChangelogItem(String field, String changelogItem) {
        if (changelogItem == null) {
            return field;
        }
        var value = field == null ? "" : field;
        List<String> list = new ArrayList<>(Arrays.asList(value.split(", ")));
        list.removeIf(element -> element.equals(changelogItem));

        return String.join(", ", list);
    }

    public String addFieldChangelogItem(String field, String changelogItem) {
        if (changelogItem == null) {
            return field;
        }
        var value = field == null ? "" : field;
        List<String> list = new ArrayList<>(Arrays.asList(value.split(", ")));
        list.add(changelogItem);

        return String.join(", ", list);
    }

    public void removeFieldHtmlTags(@NonNull Map<String, Object> fields) {
        for (var entry : fields.entrySet()) {
            var value = entry.getValue();

            if (value instanceof TemplateField templateField) {
                var stringValue = templateField.getStringValue();
                if (stringValue != null) {
                    templateField.setStringValue(removeHtmlTags(stringValue));
                }
            }
        }
    }
}
