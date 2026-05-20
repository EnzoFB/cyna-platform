package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @NoHtml(message = "Name must not contain HTML")
        String name,

        @NotNull(message = "Translations are required")
        Map<String, CategoryTranslation> translations
) {}
