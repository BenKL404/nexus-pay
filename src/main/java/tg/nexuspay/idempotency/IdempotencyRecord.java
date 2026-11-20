package tg.nexuspay.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Mémorise chaque clé d'idempotence reçue, l'empreinte de la requête et le paiement créé.
 * Si le marchand renvoie la même requête (coupure réseau, timeout), il récupère le même paiement
 * au lieu de débiter le client deux fois.
 */
@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord implements Persistable<String> {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Toujours un INSERT pour un nouvel enregistrement : en cas de requêtes simultanées avec la même clé,
     * la seconde échoue sur la clé primaire au lieu d'écraser la première (ce que ferait un merge).
     */
    @Transient
    private boolean isNew = true;

    protected IdempotencyRecord() {
        // JPA
    }

    public IdempotencyRecord(String key, String requestHash, UUID paymentId) {
        this.key = key;
        this.requestHash = requestHash;
        this.paymentId = paymentId;
        this.createdAt = Instant.now();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() { return key; }

    @Override
    public boolean isNew() { return isNew; }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public UUID getPaymentId() { return paymentId; }
    public Instant getCreatedAt() { return createdAt; }
}
