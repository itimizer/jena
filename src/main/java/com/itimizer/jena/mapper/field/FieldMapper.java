package com.itimizer.jena.mapper.field;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraIssueField;
import com.itimizer.jena.domain.TemplateField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Converts a {@link JiraIssueField} into a {@link TemplateField} by dispatching to the
 * {@link BasicFieldMapper} registered for its custom type. A field with no dedicated mapper falls
 * back to its raw value (JSON as-is), and a null value yields an empty field.
 */
@Service
@RequiredArgsConstructor
public class FieldMapper {

    private final ApplicationProperties applicationProperties;

    public TemplateField convertToTemplateField(JiraIssueField field) {
        if (field.getValue() == null) {
            return new TemplateField();
        }
        var fieldMapper = FieldTypeMapper.fieldTypeMappers().get(field.getCustom());
        if (fieldMapper == null) {
            return new TemplateField(null,
                    field.getValue().toString().replaceAll("^\"|\"$", ""));
        }
        if (fieldMapper.isExernalProperties()) {
            fieldMapper.setApplicationProperties(applicationProperties);
        }
        return new TemplateField(
                fieldMapper.toValue(field.getValue()),
                fieldMapper.toStringValue(field.getValue()));
    }
}
