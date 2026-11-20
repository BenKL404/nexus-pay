package tg.nexuspay.provider;

import java.util.UUID;
import org.springframework.stereotype.Component;
import tg.nexuspay.payment.Payment;

/**
 * Simulateur T-Money : renvoie une référence sans appeler de vraie API.
 * À remplacer par un vrai client HTTP quand les accès sandbox seront disponibles.
 */
@Component
public class MockTMoneyProvider implements PaymentProvider {

    @Override
    public ProviderCode code() {
        return ProviderCode.TMONEY;
    }

    @Override
    public String initiate(Payment payment) {
        return "TM-" + UUID.randomUUID();
    }
}
