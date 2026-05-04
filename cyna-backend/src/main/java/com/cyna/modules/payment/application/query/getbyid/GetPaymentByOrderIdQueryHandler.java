package com.cyna.modules.payment.application.query.getbyid;

import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetPaymentByOrderIdQueryHandler
        implements QueryHandler<GetPaymentByOrderIdQuery, PaymentReadModel> {

    private final PaymentRepository paymentRepository;

    public GetPaymentByOrderIdQueryHandler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentReadModel handle(GetPaymentByOrderIdQuery query) {
        return paymentRepository.findByOrderId(query.orderId())
                .filter(p -> p.getUserId().equals(query.userId()))
                .map(PaymentReadModel::from)
                .orElse(null);
    }
}
