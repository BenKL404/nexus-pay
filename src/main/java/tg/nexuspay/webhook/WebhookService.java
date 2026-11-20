package tg.nexuspay.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tg.nexuspay.audit.AuditService;
import tg.nexuspay.common.BusinessException;
import tg.nexuspay.common.Hashing;
import tg.nexuspay.common.NotFoundException;
import tg.nexuspay.payment.Payment;
import tg.nexuspay.payment.PaymentRepository;
import tg.nexuspay.provider.ProviderCode;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookProperties properties;
    private final PaymentRepository payments;
    private final AuditService audit;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookProperties properties, PaymentRepository payments,
                          AuditService audit, ObjectMapper objectMapper) {
        this.properties = properties;
        this.payments = payments;
        this.audit = audit;
        this.objectMapper = objectMapper;
    }

    /**
     * Traite une notification d'opérateur.
     * <ol>
     *   <li>Vérifie la signature HMAC-SHA256 du corps brut avec le secret de l'opérateur.</li>
     *   <li>Retrouve le paiement par la référence opérateur.</li>
     *   <li>Applique le statut final. Une notification rejouée avec le même statut est ignorée sans erreur.</li>
     * </ol>
     */
    @Transactional
    public Payment handle(ProviderCode provider, byte[] rawBody, String signature) {
        String expected = Hashing.hmacSha256Hex(properties.secretFor(provider), rawBody);
        if (!Hashing.constantTimeEquals(expected, signature)) {
            throw new InvalidSignatureException();
        }

        WebhookNotification notification = parse(rawBody);
        Payment payment = payments.findByProviderReference(notification.providerReference())
                .filter(p -> p.getProvider() == provider)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun paiement pour la référence " + notification.providerReference()));

        if (payment.getStatus() == notification.status()) {
            return payment; // notification rejouée : rien à faire
        }
        if (payment.getStatus().isFinal()) {
            log.warn("Webhook en conflit pour le paiement {} : statut actuel={}, reçu={}",
                    payment.getId(), payment.getStatus(), notification.status());
            throw new BusinessException("WEBHOOK_STATUS_CONFLICT",
                    "Le paiement est déjà " + payment.getStatus() + ".");
        }

        payment.complete(notification.status());
        audit.record("PAYMENT_" + notification.status(), payment.getId(), "via webhook " + provider);
        return payment;
    }

    private WebhookNotification parse(byte[] rawBody) {
        try {
            WebhookNotification n = objectMapper.readValue(rawBody, WebhookNotification.class);
            if (n.providerReference() == null || n.status() == null) {
                throw new BusinessException("INVALID_WEBHOOK", "providerReference et status sont obligatoires.");
            }
            return n;
        } catch (IOException e) {
            throw new BusinessException("INVALID_WEBHOOK", "Corps de notification illisible.");
        }
    }
}
