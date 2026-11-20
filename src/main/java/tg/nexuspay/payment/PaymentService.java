package tg.nexuspay.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tg.nexuspay.api.CreatePaymentRequest;
import tg.nexuspay.audit.AuditService;
import tg.nexuspay.common.BusinessException;
import tg.nexuspay.common.Hashing;
import tg.nexuspay.common.NotFoundException;
import tg.nexuspay.idempotency.IdempotencyRecord;
import tg.nexuspay.idempotency.IdempotencyRecordRepository;
import tg.nexuspay.provider.ProviderRegistry;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final ProviderRegistry providers;
    private final AuditService audit;

    public PaymentService(PaymentRepository payments, IdempotencyRecordRepository idempotencyRecords,
                          ProviderRegistry providers, AuditService audit) {
        this.payments = payments;
        this.idempotencyRecords = idempotencyRecords;
        this.providers = providers;
        this.audit = audit;
    }

    public record CreateResult(Payment payment, boolean replayed) {
    }

    /**
     * Crée un paiement de façon idempotente.
     * <ul>
     *   <li>Clé inconnue : on crée le paiement et on mémorise la clé.</li>
     *   <li>Clé connue, même requête : on renvoie le paiement existant, sans rien recréer.</li>
     *   <li>Clé connue, requête différente : refus (la clé a été réutilisée par erreur).</li>
     * </ul>
     * Deux requêtes simultanées avec la même clé : la contrainte de clé primaire fait échouer la seconde.
     */
    @Transactional
    public CreateResult create(String idempotencyKey, CreatePaymentRequest request) {
        String requestHash = Hashing.sha256Hex(request.fingerprint());

        Optional<IdempotencyRecord> existing = idempotencyRecords.findById(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new BusinessException("IDEMPOTENCY_KEY_REUSED",
                        "Cette clé d'idempotence a déjà été utilisée pour une autre requête.");
            }
            return new CreateResult(get(record.getPaymentId()), true);
        }

        Payment payment = Payment.create(request.amount(), request.provider(),
                request.payerPhone(), request.merchantReference());
        payments.save(payment);
        idempotencyRecords.saveAndFlush(new IdempotencyRecord(idempotencyKey, requestHash, payment.getId()));

        // Simplification assumée : l'appel à l'opérateur se fait dans la transaction.
        // Étape suivante prévue : l'externaliser (outbox) pour ne pas tenir de verrou pendant un appel réseau.
        String providerReference = providers.get(payment.getProvider()).initiate(payment);
        payment.attachProviderReference(providerReference);

        audit.record("PAYMENT_CREATED", payment.getId(),
                "provider=" + payment.getProvider() + " amount=" + payment.getAmount() + " " + payment.getCurrency());
        return new CreateResult(payment, false);
    }

    @Transactional(readOnly = true)
    public Payment get(UUID id) {
        return payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement introuvable : " + id));
    }
}
