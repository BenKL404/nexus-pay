package tg.nexuspay.api;

import java.util.Locale;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tg.nexuspay.common.NotFoundException;
import tg.nexuspay.provider.ProviderCode;
import tg.nexuspay.webhook.WebhookService;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    public static final String SIGNATURE_HEADER = "X-NexusPay-Signature";

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Le corps est lu en octets bruts : la signature porte sur les octets exacts envoyés par l'opérateur.
     */
    @PostMapping("/{provider}")
    public PaymentResponse receive(@PathVariable String provider,
                                   @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
                                   @RequestBody byte[] rawBody) {
        return PaymentResponse.from(webhookService.handle(parse(provider), rawBody, signature));
    }

    private ProviderCode parse(String provider) {
        try {
            return ProviderCode.valueOf(provider.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Opérateur inconnu : " + provider);
        }
    }
}
