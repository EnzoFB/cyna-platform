package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

/**
 * @param activeOnly when {@code TRUE}, an inactive category is hidden (public access);
 *                   {@code null}/{@code FALSE} returns it regardless (admin access).
 */
public record GetCategoryByIdQuery(UUID id, Boolean activeOnly) implements Query<CategoryReadModel> {}
