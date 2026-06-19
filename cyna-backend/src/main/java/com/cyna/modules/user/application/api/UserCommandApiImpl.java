package com.cyna.modules.user.application.api;

import com.cyna.modules.user.domain.event.UserErased;
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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * User-side implementation of the RGPD erasure policy. Holds all the
 * {@code user.domain} access so that callers (the {@code account} orchestrator)
 * never depend on the user module's internals. In both branches every session
 * and device-trust is revoked so no access can outlive the erasure request.
 */
@Service
class UserCommandApiImpl implements UserCommandApi {

    private static final Logger log = LoggerFactory.getLogger(UserCommandApiImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final AddressRepository addressRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    UserCommandApiImpl(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TrustedDeviceRepository trustedDeviceRepository,
                       AddressRepository addressRepository,
                       DomainEventPublisher eventPublisher,
                       TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.addressRepository = addressRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> eraseAccount(UUID userId, boolean hasTransactionalFootprint) {
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure("User not found");
        }
        User user = userOpt.get();

        transactionRunner.run(() -> {
            if (hasTransactionalFootprint) {
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

                eventPublisher.publishAll(anonymized.getDomainEvents());
                anonymized.clearDomainEvents();
                log.info("RGPD erasure: account {} anonymized (legal footprint retained)", userId);
            } else {
                // Full erasure — no legal basis to keep anything. On the shared
                // database the FK ON DELETE CASCADE chain still purges the
                // dependent rows in other modules. We ALSO publish UserErased so
                // each owning module can purge reactively through the integration
                // seam — the split-time replacement for that cascade. Both run
                // today (the reactive purge is idempotent); at a DB split the
                // cascade is dropped and the event path takes over unchanged.
                refreshTokenRepository.deleteAllByUserId(userId);
                userRepository.deleteById(userId);
                eventPublisher.publish(new UserErased(userId, Instant.now()));
                log.info("RGPD erasure: account {} hard-deleted (no transactional footprint)", userId);
            }
        });

        return Result.success();
    }
}
