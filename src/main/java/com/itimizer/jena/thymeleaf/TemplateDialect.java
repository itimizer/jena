package com.itimizer.jena.thymeleaf;

import org.springframework.stereotype.Component;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * Custom Thymeleaf dialect ({@code JenaDialect}) that registers the {@code #jena} expression
 * object, exposing JENA's date helpers to templates.
 */
@Component
public class TemplateDialect extends AbstractDialect implements IExpressionObjectDialect {

    private final TemplateExpressionObjectFactory templateExpressionObjectFactory;

    public TemplateDialect(TemplateExpressionObjectFactory templateExpressionObjectFactory) {
        super("JenaDialect");
        this.templateExpressionObjectFactory = templateExpressionObjectFactory;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return templateExpressionObjectFactory;
    }
}
