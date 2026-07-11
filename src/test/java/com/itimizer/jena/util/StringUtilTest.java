package com.itimizer.jena.util;

import com.itimizer.jena.domain.TemplateField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StringUtil Tests")
class StringUtilTest {

    private final StringUtil stringUtil = new StringUtil();

    @Nested
    @DisplayName("removeHtmlTags() method tests")
    class RemoveHtmlTagsTests {

        @Test
        @DisplayName("should remove HTML tags from string")
        void should_remove_html_tags() {
            String htmlString = "<p>Hello <b>World</b></p>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("should remove nested HTML tags")
        void should_remove_nested_html_tags() {
            String htmlString = "<div><p>Nested <span>content</span></p></div>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEqualTo("Nested content");
        }

        @Test
        @DisplayName("should remove HTML tags with attributes")
        void should_remove_html_tags_with_attributes() {
            String htmlString = "<a href=\"https://example.com\" class=\"link\">Click here</a>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEqualTo("Click here");
        }

        @Test
        @DisplayName("should handle string with no HTML tags")
        void should_handle_string_with_no_tags() {
            String plainString = "Plain text without tags";

            String result = stringUtil.removeHtmlTags(plainString);
            assertThat(result).isEqualTo("Plain text without tags");
        }

        @Test
        @DisplayName("should handle self-closing tags")
        void should_handle_self_closing_tags() {
            String htmlString = "Line 1<br/>Line 2<img src=\"image.png\"/>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEqualTo("Line 1Line 2");
        }

        @Test
        @DisplayName("should return empty string for null input")
        void should_return_empty_string_for_null() {
            String result = stringUtil.removeHtmlTags(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void should_return_empty_string_for_empty_input() {
            String result = stringUtil.removeHtmlTags("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle only HTML tags")
        void should_handle_only_html_tags() {
            String htmlString = "<p></p><div></div>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should preserve spaces within content")
        void should_preserve_spaces_within_content() {
            String htmlString = "<p>Hello   World</p>";

            String result = stringUtil.removeHtmlTags(htmlString);
            assertThat(result).isEqualTo("Hello   World");
        }
    }

    @Nested
    @DisplayName("getBaseUrl() method tests")
    class GetBaseUrlTests {

        @Test
        @DisplayName("should extract base URL from valid URL")
        void should_extract_base_url() {
            String url = "https://example.com/path/to/page";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("https://example.com/");
        }

        @Test
        @DisplayName("should handle URL with query parameters")
        void should_handle_url_with_query_parameters() {
            String url = "https://example.com/path?param=value";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("https://example.com/");
        }

        @Test
        @DisplayName("should handle URL with fragment")
        void should_handle_url_with_fragment() {
            String url = "https://example.com/path#section";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("https://example.com/");
        }

        @Test
        @DisplayName("should handle HTTP URL")
        void should_handle_http_url() {
            String url = "http://example.com/page";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("http://example.com/");
        }

        @Test
        @DisplayName("should handle URL with port")
        void should_handle_url_with_port() {
            String url = "https://example.com:8080/path";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("https://example.com:8080/");
        }

        @Test
        @DisplayName("should return empty string for invalid URL")
        void should_return_null_for_invalid_url() {
            String url = "not a valid url";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for null input")
        void should_return_null_for_null_input() {
            String result = stringUtil.getBaseUrl(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle URL with subdomain")
        void should_handle_url_with_subdomain() {
            String url = "https://sub.example.com/path";

            String result = stringUtil.getBaseUrl(url);
            assertThat(result).isEqualTo("https://sub.example.com/");
        }
    }

    @Nested
    @DisplayName("getHostUrl() method tests")
    class GetHostUrlTests {

        @Test
        @DisplayName("should extract host from valid URL")
        void should_extract_host() {
            String url = "https://example.com/path/to/page";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should extract host from URL with query parameters")
        void should_extract_host_with_query_parameters() {
            String url = "https://example.com/path?param=value";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should extract host from URL with fragment")
        void should_extract_host_with_fragment() {
            String url = "https://example.com/path#section";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should extract host from HTTP URL")
        void should_extract_host_from_http() {
            String url = "http://example.com/page";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should extract host from URL with port")
        void should_extract_host_with_port() {
            String url = "https://example.com:8080/path";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should extract host from URL with subdomain")
        void should_extract_host_with_subdomain() {
            String url = "https://sub.example.com/path";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEqualTo("sub.example.com");
        }

        @Test
        @DisplayName("should return empty string for invalid URL")
        void should_return_null_for_invalid_url() {
            String url = "not a valid url";

            String result = stringUtil.getHostUrl(url);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for null input")
        void should_return_null_for_null_input() {
            String result = stringUtil.getHostUrl(null);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeFieldChangelogItem() method tests")
    class RemoveFieldChangelogItemTests {

        @Test
        @DisplayName("should remove item from changelog field")
        void should_remove_item_from_changelog() {
            String field = "item1, item2, item3";

            String result = stringUtil.removeFieldChangelogItem(field, "item2");
            assertThat(result).isEqualTo("item1, item3");
        }

        @Test
        @DisplayName("should remove only item from changelog field")
        void should_remove_only_item() {
            String field = "item1";

            String result = stringUtil.removeFieldChangelogItem(field, "item1");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle item not in changelog field")
        void should_handle_item_not_in_field() {
            String field = "item1, item2, item3";

            String result = stringUtil.removeFieldChangelogItem(field, "item4");
            assertThat(result).isEqualTo("item1, item2, item3");
        }

        @Test
        @DisplayName("should return field when changelogItem is null")
        void should_return_field_when_changelog_item_null() {
            String field = "item1, item2, item3";

            String result = stringUtil.removeFieldChangelogItem(field, null);
            assertThat(result).isEqualTo("item1, item2, item3");
        }

        @Test
        @DisplayName("should handle empty field")
        void should_handle_empty_field() {
            String field = "";

            String result = stringUtil.removeFieldChangelogItem(field, "item1");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle null field")
        void should_handle_null_field() {
            String result = stringUtil.removeFieldChangelogItem(null, "item1");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return null when both field and changelogItem are null")
        void should_return_null_when_field_and_changelog_item_null() {
            String result = stringUtil.removeFieldChangelogItem(null, null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle field with spaces around items")
        void should_handle_field_with_spaces() {
            String field = "item1 , item2 , item3";

            String result = stringUtil.removeFieldChangelogItem(field, "item2 ");
            assertThat(result).isEqualTo("item1 , item3");
        }
    }

    @Nested
    @DisplayName("addFieldChangelogItem() method tests")
    class AddFieldChangelogItemTests {

        @Test
        @DisplayName("should add item to changelog field")
        void should_add_item_to_changelog() {
            String field = "item1, item2";

            String result = stringUtil.addFieldChangelogItem(field, "item3");
            assertThat(result).isEqualTo("item1, item2, item3");
        }

        @Test
        @DisplayName("should add item to empty field")
        void should_add_item_to_empty_field() {
            String field = "";

            String result = stringUtil.addFieldChangelogItem(field, "item1");
            assertThat(result).isEqualTo(", item1");
        }

        @Test
        @DisplayName("should add item to null field")
        void should_add_item_to_null_field() {
            String result = stringUtil.addFieldChangelogItem(null, "item1");
            assertThat(result).isEqualTo(", item1");
        }

        @Test
        @DisplayName("should add duplicate item to changelog field")
        void should_add_duplicate_item() {
            String field = "item1, item2";

            String result = stringUtil.addFieldChangelogItem(field, "item1");
            assertThat(result).isEqualTo("item1, item2, item1");
        }

        @Test
        @DisplayName("should add item to field with single item")
        void should_add_item_to_single_item_field() {
            String field = "item1";

            String result = stringUtil.addFieldChangelogItem(field, "item2");
            assertThat(result).isEqualTo("item1, item2");
        }

        @Test
        @DisplayName("should return field when changelogItem is null")
        void should_return_field_when_changelog_item_null() {
            String field = "item1, item2";

            String result = stringUtil.addFieldChangelogItem(field, null);
            assertThat(result).isEqualTo("item1, item2");
        }

        @Test
        @DisplayName("should add item with special characters")
        void should_add_item_with_special_characters() {
            String field = "item1, item2";

            String result = stringUtil.addFieldChangelogItem(field, "item-3");
            assertThat(result).isEqualTo("item1, item2, item-3");
        }

        @Test
        @DisplayName("should add numeric item to changelog field")
        void should_add_numeric_item() {
            String field = "item1, item2";

            String result = stringUtil.addFieldChangelogItem(field, "123");
            assertThat(result).isEqualTo("item1, item2, 123");
        }
    }

    @Nested
    @DisplayName("removeFieldHtmlTags() method tests")
    class RemoveFieldHtmlTagsTests {

        @Test
        @DisplayName("should remove simple HTML tags from fields")
        void should_remove_simple_html_tags() {
            Map<String, Object> fields = new HashMap<>();
            fields.put("summary", new TemplateField(
                    null,
                    "Issue Summary"
            ));
            fields.put("description", new TemplateField(
                    null,
                    "<p><b>Description</b> with <a href='link'>link</a></p>"
            ));
            fields.put("customfield_10000", new TemplateField(
                    null,
                    "<div><span>Nested</span> tags</div>"
            ));

            stringUtil.removeFieldHtmlTags(fields);
            assertThat(((TemplateField) fields.get("summary"))
                    .getStringValue()).isEqualTo("Issue Summary");
            assertThat(((TemplateField) fields.get("description"))
                    .getStringValue()).isEqualTo("Description with link");
            assertThat(((TemplateField) fields.get("customfield_10000"))
                    .getStringValue()).isEqualTo("Nested tags");
        }

        @Test
        @DisplayName("should handle empty string")
        void should_handle_empty_string() {
            Map<String, Object> fields = new HashMap<>();
            fields.put("description", new TemplateField(
                    null,
                    ""
            ));

            stringUtil.removeFieldHtmlTags(fields);
            TemplateField result = (TemplateField) fields.get("description");
            assertThat(result.getStringValue()).isEmpty();
        }

        @Test
        @DisplayName("should handle null string value")
        void should_handle_null_string_value() {
            Map<String, Object> fields = new HashMap<>();
            fields.put("description", new TemplateField(
                    null,
                    null
            ));

            stringUtil.removeFieldHtmlTags(fields);
            TemplateField result = (TemplateField) fields.get("description");
            assertThat(result.getStringValue()).isNull();
        }

        @Test
        @DisplayName("should throw NullPointerException when map is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_map_is_null() {
            assertThatThrownBy(() -> stringUtil.removeFieldHtmlTags(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("truncate() method tests")
    class TruncateTests {

        @Test
        @DisplayName("should return empty string for null input")
        void should_return_empty_string_for_null() {
            assertThat(StringUtil.truncate(null, 10)).isEmpty();
        }

        @Test
        @DisplayName("should return value unchanged when shorter than max length")
        void should_return_value_unchanged_when_shorter() {
            assertThat(StringUtil.truncate("short body", 100)).isEqualTo("short body");
        }

        @Test
        @DisplayName("should return value unchanged when equal to max length")
        void should_return_value_unchanged_when_equal() {
            String value = "exactly-ten";
            assertThat(StringUtil.truncate(value, value.length())).isEqualTo(value);
        }

        @Test
        @DisplayName("should truncate and append marker when longer than max length")
        void should_truncate_when_longer() {
            String value = "0123456789ABCDEF";

            String result = StringUtil.truncate(value, 10);
            assertThat(result).isEqualTo("0123456789... (truncated 6 chars)");
        }
    }
}
