package tg.nexuspay.webhook;

public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException() {
        super("Signature du webhook invalide.");
    }
}
