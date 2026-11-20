package tg.nexuspay.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tg.nexuspay.audit.AuditEvent;
import tg.nexuspay.audit.AuditEventRepository;
import tg.nexuspay.payment.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final PaymentService paymentService;
    private final AuditEventRepository auditEvents;

    public PaymentController(PaymentService paymentService, AuditEventRepository auditEvents) {
        this.paymentService = paymentService;
        this.auditEvents = auditEvents;
    }

    /**
     * 201 à la création, 200 avec l'en-tête Idempotent-Replayed quand la même requête est rejouée.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentService.CreateResult result = paymentService.create(idempotencyKey, request);
        PaymentResponse body = PaymentResponse.from(result.payment());
        if (result.replayed()) {
            return ResponseEntity.ok().header(REPLAYED_HEADER, "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/v1/payments/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.get(id));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@PathVariable UUID id) {
        paymentService.get(id); // 404 si le paiement n'existe pas
        return auditEvents.findByPaymentIdOrderByIdAsc(id).stream().map(AuditEntry::from).toList();
    }

    public record AuditEntry(java.time.Instant occurredAt, String action, String details) {
        static AuditEntry from(AuditEvent e) {
            return new AuditEntry(e.getOccurredAt(), e.getAction(), e.getDetails());
        }
    }
}
