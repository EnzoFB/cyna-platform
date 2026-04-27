package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetCategoryByIdQuery(UUID id) implements Query<CategoryReadModel> {}
