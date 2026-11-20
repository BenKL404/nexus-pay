package tg.nexuspay.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import tg.nexuspay.common.BusinessException;
import tg.nexuspay.provider.ProviderCode;

/**
 * Un paiement Mobile Money.
 * <p>
 * Le montant est stocké en francs CFA (XOF), qui n'ont pas de centimes : un {@code long} suffit
 * et évite toute erreur d'arrondi.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProviderCode provider;

    @Column(name = "payer_phone", nullable = false, length = 20)
    private String payerPhone;

    @Column(name = "merchant_reference", nullable = false, length = 64)
    private String merchantReference;

    @Column(name = "provider_reference", unique = true, length = 64)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version; // null tant que non persisté : Spring Data fait alors un INSERT, pas un merge

    protected Payment() {
        // JPA
    }

    public static Payment create(long amount, ProviderCode provider, String payerPhone, String merchantReference) {
        if (amount <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Le montant doit être strictement positif.");
        }
        Payment p = new Payment();
        p.id = UUID.randomUUID();
        p.amount = amount;
        p.currency = "XOF";
        p.provider = provider;
        p.payerPhone = payerPhone;
        p.merchantReference = merchantReference;
        p.status = PaymentStatus.PENDING;
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void attachProviderReference(String reference) {
        this.providerReference = reference;
        this.updatedAt = Instant.now();
    }

    /**
     * Passe le paiement à un statut final. Refuse toute transition depuis un statut déjà final.
     */
    public void complete(PaymentStatus newStatus) {
        if (!newStatus.isFinal()) {
            throw new BusinessException("INVALID_TRANSITION", "Le nouveau statut doit être SUCCESS ou FAILED.");
        }
        if (status.isFinal()) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Le paiement est déjà " + status + " et ne peut plus changer.");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public ProviderCode getProvider() { return provider; }
    public String getPayerPhone() { return payerPhone; }
    public String getMerchantReference() { return merchantReference; }
    public String getProviderReference() { return providerReference; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
