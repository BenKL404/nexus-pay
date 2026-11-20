package tg.nexuspay.api;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tg.nexuspay.common.BusinessException;
import tg.nexuspay.common.NotFoundException;
import tg.nexuspay.webhook.InvalidSignatureException;

/**
 * Toutes les erreurs sont renvoyées au format RFC 9457 (Problem Details) avec un code stable.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail business(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case "INVALID_TRANSITION", "WEBHOOK_STATUS_CONFLICT" -> HttpStatus.CONFLICT;
            case "INVALID_WEBHOOK" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return problem(status, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidSignatureException.class)
    public ProblemDetail invalidSignature(InvalidSignatureException e) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ProblemDetail invalidParameter(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Paramètre invalide.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail missingHeader(MissingRequestHeaderException e) {
        return problem(HttpStatus.BAD_REQUEST, "MISSING_HEADER", "En-tête obligatoire manquant : " + e.getHeaderName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Corps de requête illisible.");
    }

    /**
     * Deux requêtes simultanées avec la même clé d'idempotence : la seconde est refusée, le client peut réessayer.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_REQUEST",
                "Une requête identique est en cours de traitement. Réessayez dans un instant.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return pd;
    }
}
