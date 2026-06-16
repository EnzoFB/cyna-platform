package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.repository.ProcessedStripeEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaProcessedStripeEventRepositoryAdapter implements ProcessedStripeEventRepository {

    private final SpringDataProcessedStripeEventRepository springRepo;

    JpaProcessedStripeEventRepositoryAdapter(SpringDataProcessedStripeEventRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public boolean isAlreadyProcessed(String eventId) {
        return springRepo.existsById(eventId);
    }

    // REQUIRED: the webhook handler runs outside a transaction, so this opens
    // its own short tx just to commit the ledger row. The @Modifying native
    // INSERT requires an active transaction.
    @Override
    @Transactional
    public void markProcessed(String eventId, String eventType) {
        // INSERT ... ON CONFLICT DO NOTHING — idempotent and concurrency-safe:
        // a racing duplicate that also gets here is a no-op, the database
        // arbitrates instead of application code.
        springRepo.insertIfAbsent(eventId, eventType);
    }
}
