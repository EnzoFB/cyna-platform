package com.cyna.modules.product.application.query.listcategories;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.shared.application.Query;

import java.util.List;

public record ListCategoriesQuery(Boolean activeOnly) implements Query<List<CategoryReadModel>> {}
