package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @NoHtml(message = "Name must not contain HTML")
        String name,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        @NoHtml(message = "Full name must not contain HTML")
        String fullName,

        @Size(max = 255, message = "Full name (EN) must not exceed 255 characters")
        @NoHtml(message = "Full name (EN) must not contain HTML")
        String fullNameEn,

        @NoHtml(message = "Description must not contain HTML")
        String description,

        @NoHtml(message = "Description (EN) must not contain HTML")
        String descriptionEn
) {}
