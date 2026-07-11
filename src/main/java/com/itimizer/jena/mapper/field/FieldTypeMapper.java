package com.itimizer.jena.mapper.field;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry mapping each supported Jira custom-field type key to the {@link BasicFieldMapper} that
 * handles it. {@link #fieldTypeMappers()} exposes this as a lookup keyed by the type string.
 */
@Getter
public enum FieldTypeMapper {

    TEXTFIELD("com.atlassian.jira.plugin.system.customfieldtypes:textfield",
            new StringFieldMapper()),
    TEXTAREA("com.atlassian.jira.plugin.system.customfieldtypes:textarea",
            new StringFieldMapper()),
    SELECT("com.atlassian.jira.plugin.system.customfieldtypes:select",
            new OptionFieldMapper()),
    MULTISELECT("com.atlassian.jira.plugin.system.customfieldtypes:multiselect",
            new MultiSelectFieldMapper()),
    RADIOBUTTON("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons",
            new OptionFieldMapper()),
    CHECKBOX("com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes",
            new MultiSelectFieldMapper()),
    DATE("com.atlassian.jira.plugin.system.customfieldtypes:datepicker",
            new DateFieldMapper()),
    DATETIME("com.atlassian.jira.plugin.system.customfieldtypes:datetime",
            new DatetimeFieldMapper()),
    FLOAT("com.atlassian.jira.plugin.system.customfieldtypes:float",
            new StringFieldMapper()),
    USERPICKER("com.atlassian.jira.plugin.system.customfieldtypes:userpicker",
            new UserFieldMapper()),
    MULTIUSERPICKER("com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker",
            new MultiUserFieldMapper()),
    INSIGHT("com.riadalabs.jira.plugins.insight:rlabs-customfield-default-object",
            new InsightFieldMapper());

    public final String custom;
    public final BasicFieldMapper fieldMapper;

    FieldTypeMapper(String custom, BasicFieldMapper fieldMapper) {
        this.custom = custom;
        this.fieldMapper = fieldMapper;
    }

    public static Map<String, BasicFieldMapper> fieldTypeMappers() {
        return java.util.Arrays.stream(FieldTypeMapper.values())
                .collect(Collectors.toMap(FieldTypeMapper::getCustom,
                        FieldTypeMapper::getFieldMapper));
    }
}
