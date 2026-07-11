package com.itimizer.jena.thymeleaf;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Set;

/**
 * Binds the {@code #jena} expression name to the {@link TemplateUtil} helper instance, making its
 * date methods callable from templates as {@code #jena.formatZonedDateTime(...)} etc.
 */
@Component
public class TemplateExpressionObjectFactory implements IExpressionObjectFactory {

    private final TemplateUtil templateUtil;

    public TemplateExpressionObjectFactory(TemplateUtil templateUtil) {
        this.templateUtil = templateUtil;
    }

    @Override
    public Set<String> getAllExpressionObjectNames() {
        return Set.of("jena");
    }

    @Override
    public Object buildObject(IExpressionContext context, String expressionObjectName) {
        return templateUtil;
    }

    @Override
    public boolean isCacheable(final String expressionObjectName) {
        return true;
    }
}
