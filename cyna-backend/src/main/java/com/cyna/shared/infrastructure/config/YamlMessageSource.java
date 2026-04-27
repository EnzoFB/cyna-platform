package com.cyna.shared.infrastructure.config;

import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

public class YamlMessageSource extends AbstractMessageSource {

    private final Map<String, Object> messages;

    public YamlMessageSource(String basename, Locale locale) {
        try {
            String fileName = basename + "_" + locale.getLanguage() + ".yaml";
            InputStream inputStream = new ClassPathResource(fileName).getInputStream();
            Yaml yaml = new Yaml();
            messages = yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger le fichier YAML", e);
        }
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        Object val = getMessageFromMap(code, messages);
        if (val == null) return null;
        return new MessageFormat(val.toString(), locale);
    }

    private Object getMessageFromMap(String code, Map<String, Object> map) {
        String[] keys = code.split("\\.");
        Map<String, Object> current = map;
        Object result = null;

        for (String key : keys) {
            Object value = current.get(key);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                result = value;
                break;
            }
        }

        return result;
    }
}
