package com.cyna.modules.account.application.command.delete;

import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.user.application.api.UserCommandApi;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RGPD Art. 17 — single erasure policy shared by the self-service
 * ({@code DELETE /api/v1/account}) and admin
 * ({@code DELETE /api/v1/admin/users/{id}}) paths.
 *
 * <p>Orchestration only: the user-owned data is erased through
 * {@link UserCommandApi}, and the lawful outcome depends on whether the account
 * carries a legally-retained transactional footprint:
 * <ul>
 *   <li><b>No orders</b> → no legal basis to keep anything → hard delete.</li>
 *   <li><b>Has orders</b> → French Code de commerce L123-22 obliges keeping the
 *       accounting records (10 years) → anonymize-and-keep, and the local
 *       card-metadata cache is purged on the payment side (data minimization).</li>
 * </ul>
 * Keeping this orchestration in the {@code account} module (rather than in
 * {@code user}) keeps the user module a leaf, so the module graph stays acyclic.
 */
@Component
public class AccountErasure {

    private final UserCommandApi userCommandApi;
    private final OrderQueryApi orderQueryApi;
    private final PaymentCommandApi paymentCommandApi;

    public AccountErasure(UserCommandApi userCommandApi,
                          OrderQueryApi orderQueryApi,
                          PaymentCommandApi paymentCommandApi) {
        this.userCommandApi = userCommandApi;
        this.orderQueryApi = orderQueryApi;
        this.paymentCommandApi = paymentCommandApi;
    }

    public Result<Void> erase(UUID userId) {
        // Orders are the legally-retained accounting records; their presence is
        // the authoritative "has a transactional footprint" check.
        boolean hasRetainedFootprint = orderQueryApi.userHasOrders(userId);

        Result<Void> result = userCommandApi.eraseAccount(userId, hasRetainedFootprint);
        if (result.isFailure()) {
            return result;
        }

        if (hasRetainedFootprint) {
            // Local card-metadata cache minimization. Stripe Customer + consent
            // proof are retained (see PaymentCommandApi).
            paymentCommandApi.purgeLocalPaymentDataForUser(userId);
        }

        return result;
    }
}
