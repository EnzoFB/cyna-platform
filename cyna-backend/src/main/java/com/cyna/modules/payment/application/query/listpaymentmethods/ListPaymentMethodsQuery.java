package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.shared.application.Query;

import java.util.List;
import java.util.UUID;

public record ListPaymentMethodsQuery(UUID userId) implements Query<List<SavedPaymentMethodReadModel>> {}
