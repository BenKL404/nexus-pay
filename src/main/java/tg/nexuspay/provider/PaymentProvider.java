package tg.nexuspay.provider;

import tg.nexuspay.payment.Payment;

/**
 * Adaptateur vers un opérateur Mobile Money.
 * <p>
 * Chaque opérateur a sa propre API : on l'isole derrière cette interface pour que le reste
 * de la passerelle ne dépende d'aucun opérateur en particulier.
 */
public interface PaymentProvider {

    ProviderCode code();

    /**
     * Demande à l'opérateur d'initier le paiement (le client reçoit une demande de validation sur son téléphone).
     *
     * @return la référence de la transaction côté opérateur
     */
    String initiate(Payment payment);
}
