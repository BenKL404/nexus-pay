package tg.nexuspay.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'audit : on ajoute, on ne modifie ni ne supprime jamais.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 64, updatable = false)
    private String action;

    @Column(name = "payment_id", updatable = false)
    private UUID paymentId;

    @Column(length = 500, updatable = false)
    private String details;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(String action, UUID paymentId, String details) {
        this.occurredAt = Instant.now();
        this.action = action;
        this.paymentId = paymentId;
        this.details = details;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getAction() { return action; }
    public UUID getPaymentId() { return paymentId; }
    public String getDetails() { return details; }
}
