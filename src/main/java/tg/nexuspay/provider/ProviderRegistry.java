package tg.nexuspay.provider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tg.nexuspay.common.BusinessException;

@Component
public class ProviderRegistry {

    private final Map<ProviderCode, PaymentProvider> providers = new EnumMap<>(ProviderCode.class);

    public ProviderRegistry(List<PaymentProvider> all) {
        all.forEach(p -> providers.put(p.code(), p));
    }

    public PaymentProvider get(ProviderCode code) {
        PaymentProvider provider = providers.get(code);
        if (provider == null) {
            throw new BusinessException("PROVIDER_UNAVAILABLE", "Opérateur non disponible : " + code);
        }
        return provider;
    }
}
