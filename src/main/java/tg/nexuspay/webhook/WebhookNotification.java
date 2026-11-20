package tg.nexuspay.webhook;

import tg.nexuspay.payment.PaymentStatus;

/**
 * Notification envoyée par l'opérateur quand le client a validé (ou refusé) le paiement.
 */
public record WebhookNotification(String providerReference, PaymentStatus status) {
}
