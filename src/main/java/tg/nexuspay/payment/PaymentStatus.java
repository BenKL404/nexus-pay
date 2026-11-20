package tg.nexuspay.payment;

/**
 * Cycle de vie d'un paiement. Seul PENDING peut évoluer ; SUCCESS et FAILED sont définitifs.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public boolean isFinal() {
        return this != PENDING;
    }
}
