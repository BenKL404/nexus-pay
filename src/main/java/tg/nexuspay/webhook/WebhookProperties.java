package tg.nexuspay.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import tg.nexuspay.provider.ProviderCode;

/**
 * Secrets partagés avec chaque opérateur pour signer les notifications (webhooks).
 * En production, ils viennent de variables d'environnement, jamais du code.
 */
@ConfigurationProperties(prefix = "nexuspay.webhooks")
public record WebhookProperties(String tmoneySecret, String floozSecret) {

    public String secretFor(ProviderCode code) {
        return switch (code) {
            case TMONEY -> tmoneySecret;
            case FLOOZ -> floozSecret;
        };
    }
}
