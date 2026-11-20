package tg.nexuspay.common;

/**
 * Erreur métier attendue (règle violée), renvoyée au client avec un code stable.
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
