package com.cyna.shared.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

import java.text.MessageFormat;
import java.util.Locale;

@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine templateEngine(
            SpringResourceTemplateResolver templateResolver,
            MessageSource messageSource
    ) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);

        engine.setTemplateEngineMessageSource(messageSource);

        return engine;
    }

    @Bean
    public MessageSource messageSource() {
        return new AbstractMessageSource() {
            @Override
            protected MessageFormat resolveCode(String code, Locale locale) {
                YamlMessageSource yamlSource = new YamlMessageSource("i18n/messages", locale);
                return yamlSource.resolveCode(code, locale);
            }
        };
    }
}
