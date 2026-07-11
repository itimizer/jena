package com.itimizer.jena.config;

import com.itimizer.jena.entity.Channel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC converter that parses a {@link Channel} from a request parameter
 * case-insensitively (e.g. {@code telegram} → {@link Channel#TELEGRAM}).
 */
@Component
public class StringToChannelConverter implements Converter<String, Channel> {

    @Override
    public Channel convert(String source) {
        return Channel.valueOf(source.trim().toUpperCase());
    }
}