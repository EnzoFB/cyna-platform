package com.cyna.modules.product.application.query.listofferpromotions;

import com.cyna.shared.application.Query;

import java.util.List;

public record ListOfferPromotionsQuery(String language) implements Query<List<OfferPromotionReadModel>> {
}

