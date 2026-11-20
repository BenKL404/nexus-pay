package tg.nexuspay.api;

import java.time.Instant;
import java.util.UUID;
import tg.nexuspay.payment.Payment;
import tg.nexuspay.payment.PaymentStatus;
import tg.nexuspay.provider.ProviderCode;

public record PaymentResponse(
        UUID id,
        long amount,
        String currency,
        ProviderCode provider,
        String merchantReference,
        String providerReference,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getAmount(), p.getCurrency(), p.getProvider(),
                p.getMerchantReference(), p.getProviderReference(), p.getStatus(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
