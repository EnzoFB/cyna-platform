package com.cyna.modules.user.application.command.delete;

import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.modules.user.domain.repository.RefreshTokenRepository;
import com.cyna.modules.user.domain.repository.TrustedDeviceRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RGPD Art. 17 — single erasure policy shared by the self-service
 * ({@code DELETE /api/v1/account}) and admin
 * ({@code DELETE /api/v1/admin/users/{id}}) paths.
 *
 * <p>The lawful outcome of an erasure request is not always "delete":
 * <ul>
 *   <li><b>No transactional footprint</b> (the user never ordered) → there is
 *       no legal basis to keep anything → <b>hard delete</b>. The FK
 *       {@code ON DELETE CASCADE} chain removes addresses, trusted devices,
 *       OTP / reset / email-change tokens and the payment consent log.</li>
 *   <li><b>Has orders</b> → French Code de commerce L123-22 obliges keeping
 *       the accounting records (10 years); the account cannot be deleted
 *       without destroying legally-required data → <b>anonymize-and-keep</b>.
 *       Direct identifiers are scrubbed, the row survives only as a
 *       non-identifying FK anchor.</li>
 * </ul>
 *
 * <p>In both cases all sessions and device-trust are revoked so no access can
 * outlive the request.
 */
@Component
public class AccountErasure {

    private static final Logger log = LoggerFactory.getLogger(AccountErasure.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final AddressRepository addressRepository;
    private final OrderQueryApi orderQueryApi;
    private final PaymentCommandApi paymentCommandApi;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public AccountErasure(UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          TrustedDeviceRepository trustedDeviceRepository,
                          AddressRepository addressRepository,
                          OrderQueryApi orderQueryApi,
                          PaymentCommandApi paymentCommandApi,
                          DomainEventPublisher eventPublisher,
                          TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.addressRepository = addressRepository;
        this.orderQueryApi = orderQueryApi;
        this.paymentCommandApi = paymentCommandApi;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    public Result<Void> erase(UUID userId) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure("User not found");
        }
        User user = userOpt.get();

        boolean hasRetainedFootprint = orderQueryApi.userHasOrders(userId);

        transactionRunner.run(() -> {
            if (hasRetainedFootprint) {
                // Anonymize-and-keep: scrub PII, sever every access channel,
                // drop data with no standalone retention basis.
                refreshTokenRepository.deleteAllByUserId(userId);
                trustedDeviceRepository.deleteAllByUserId(userId);

                User anonymized = user.anonymize();
                userRepository.save(anonymized);

                // Addresses carry PII and have no standalone retention basis —
                // the billing address is retained on the Stripe-side invoices.
                addressRepository.findAllByUserId(userId)
                        .forEach(a -> addressRepository.deleteById(a.getId()));

                // Local card-metadata cache minimization. Stripe Customer +
                // consent proof are retained (see PaymentCommandApi).
                paymentCommandApi.purgeLocalPaymentDataForUser(userId);

                eventPublisher.publishAll(anonymized.getDomainEvents());
                anonymized.clearDomainEvents();
                log.info("RGPD erasure: account {} anonymized (legal footprint retained)", userId);
            } else {
                // Full erasure — no legal basis to keep anything. The FK
                // ON DELETE CASCADE chain handles the dependent rows.
                refreshTokenRepository.deleteAllByUserId(userId);
                userRepository.deleteById(userId);
                log.info("RGPD erasure: account {} hard-deleted (no transactional footprint)", userId);
            }
        });

        return Result.success();
    }
}
