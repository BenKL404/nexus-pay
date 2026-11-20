package tg.nexuspay.provider;

import java.util.UUID;
import org.springframework.stereotype.Component;
import tg.nexuspay.payment.Payment;

/**
 * Simulateur Flooz (Moov Money) : renvoie une référence sans appeler de vraie API.
 */
@Component
public class MockFloozProvider implements PaymentProvider {

    @Override
    public ProviderCode code() {
        return ProviderCode.FLOOZ;
    }

    @Override
    public String initiate(Payment payment) {
        return "FZ-" + UUID.randomUUID();
    }
}
