package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @NoHtml(message = "Name must not contain HTML")
        String name,

        @NoHtml(message = "Description must not contain HTML")
        String description
) {}
