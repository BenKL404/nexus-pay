package tg.nexuspay.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tg.nexuspay.provider.ProviderCode;

/**
 * Demande de paiement envoyée par un marchand.
 *
 * @param amount            montant en francs CFA (XOF), sans décimales
 * @param provider          opérateur Mobile Money
 * @param payerPhone        numéro togolais au format international, ex. +22890123456
 * @param merchantReference référence de la commande côté marchand
 */
public record CreatePaymentRequest(
        @NotNull @Positive Long amount,
        @NotNull ProviderCode provider,
        @NotBlank @Pattern(regexp = "^\\+228[0-9]{8}$", message = "doit être au format +228XXXXXXXX") String payerPhone,
        @NotBlank @Size(max = 64) String merchantReference) {

    /**
     * Empreinte stable de la requête, pour détecter une clé d'idempotence réutilisée avec un contenu différent.
     */
    public String fingerprint() {
        return amount + "|" + provider + "|" + payerPhone + "|" + merchantReference;
    }
}
