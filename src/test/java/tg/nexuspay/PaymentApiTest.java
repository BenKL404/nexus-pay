package tg.nexuspay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tg.nexuspay.common.Hashing;
import tg.nexuspay.payment.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiTest {

    private static final String TMONEY_SECRET = "test-tmoney-secret";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentRepository payments;

    private String body(long amount, String reference) {
        return """
                {"amount": %d, "provider": "TMONEY", "payerPhone": "+22890123456", "merchantReference": "%s"}
                """.formatted(amount, reference);
    }

    private JsonNode createPayment(String key, String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String webhook(String providerReference, String status) {
        return """
                {"providerReference": "%s", "status": "%s"}
                """.formatted(providerReference, status).trim();
    }

    @Test
    void creeUnPaiementEnAttente() throws Exception {
        JsonNode payment = createPayment(UUID.randomUUID().toString(), body(5000, "CMD-100"));

        assertThat(payment.get("status").asText()).isEqualTo("PENDING");
        assertThat(payment.get("currency").asText()).isEqualTo("XOF");
        assertThat(payment.get("providerReference").asText()).startsWith("TM-");
    }

    @Test
    void memeCleEtMemeRequeteRenvoientLeMemePaiementSansDoublon() throws Exception {
        String key = UUID.randomUUID().toString();
        String json = body(7500, "CMD-200");
        JsonNode first = createPayment(key, json);
        long countAfterFirst = payments.count();

        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(first.get("id").asText()));

        assertThat(payments.count()).isEqualTo(countAfterFirst);
    }

    @Test
    void memeCleAvecUneAutreRequeteEstRefusee() throws Exception {
        String key = UUID.randomUUID().toString();
        createPayment(key, body(1000, "CMD-300"));

        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(9999, "CMD-300")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void sansCleIdempotenceLaRequeteEstRefusee() throws Exception {
        mvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1000, "CMD-400")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void numeroDeTelephoneInvalideRefuse() throws Exception {
        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 1000, "provider": "TMONEY", "payerPhone": "90123456", "merchantReference": "CMD-500"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookSigneValideLePaiementEtLaisseUneTraceAudit() throws Exception {
        JsonNode payment = createPayment(UUID.randomUUID().toString(), body(2500, "CMD-600"));
        String id = payment.get("id").asText();
        String json = webhook(payment.get("providerReference").asText(), "SUCCESS");

        mvc.perform(post("/api/v1/webhooks/tmoney")
                        .header("X-NexusPay-Signature", sign(json))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mvc.perform(get("/api/v1/payments/" + id))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mvc.perform(get("/api/v1/payments/" + id + "/audit"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("PAYMENT_CREATED"))
                .andExpect(jsonPath("$[1].action").value("PAYMENT_SUCCESS"));
    }

    @Test
    void webhookAvecMauvaiseSignatureEstRejete() throws Exception {
        JsonNode payment = createPayment(UUID.randomUUID().toString(), body(2500, "CMD-700"));
        String json = webhook(payment.get("providerReference").asText(), "SUCCESS");

        mvc.perform(post("/api/v1/webhooks/tmoney")
                        .header("X-NexusPay-Signature", "signature-falsifiee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));

        mvc.perform(get("/api/v1/payments/" + payment.get("id").asText()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void webhookRejoueEstIgnoreEtWebhookContradictoireRefuse() throws Exception {
        JsonNode payment = createPayment(UUID.randomUUID().toString(), body(2500, "CMD-800"));
        String reference = payment.get("providerReference").asText();
        String success = webhook(reference, "SUCCESS");
        String failed = webhook(reference, "FAILED");

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/webhooks/tmoney")
                            .header("X-NexusPay-Signature", sign(success))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(success))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/v1/webhooks/tmoney")
                        .header("X-NexusPay-Signature", sign(failed))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WEBHOOK_STATUS_CONFLICT"));
    }

    private static String sign(String json) {
        return Hashing.hmacSha256Hex(TMONEY_SECRET, json.getBytes(StandardCharsets.UTF_8));
    }
}
