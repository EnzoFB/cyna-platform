package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListPaymentMethodsQueryHandler
        implements QueryHandler<ListPaymentMethodsQuery, List<SavedPaymentMethodReadModel>> {

    private final SavedPaymentMethodRepository repository;

    public ListPaymentMethodsQueryHandler(SavedPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SavedPaymentMethodReadModel> handle(ListPaymentMethodsQuery query) {
        return repository.findAllByUserId(query.userId())
                .stream()
                .map(SavedPaymentMethodReadModel::from)
                .toList();
    }
}
