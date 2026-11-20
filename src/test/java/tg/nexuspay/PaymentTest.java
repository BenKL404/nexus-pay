package tg.nexuspay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tg.nexuspay.common.BusinessException;
import tg.nexuspay.payment.Payment;
import tg.nexuspay.payment.PaymentStatus;
import tg.nexuspay.provider.ProviderCode;

class PaymentTest {

    @Test
    void nouveauPaiementEstEnAttenteEnXof() {
        Payment p = Payment.create(5000, ProviderCode.TMONEY, "+22890123456", "CMD-1");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.getCurrency()).isEqualTo("XOF");
    }

    @Test
    void montantNulOuNegatifRefuse() {
        assertThatThrownBy(() -> Payment.create(0, ProviderCode.FLOOZ, "+22890123456", "CMD-1"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Payment.create(-100, ProviderCode.FLOOZ, "+22890123456", "CMD-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unStatutFinalNeChangePlus() {
        Payment p = Payment.create(5000, ProviderCode.TMONEY, "+22890123456", "CMD-1");
        p.complete(PaymentStatus.SUCCESS);
        assertThatThrownBy(() -> p.complete(PaymentStatus.FAILED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void onNeRevientPasAPending() {
        Payment p = Payment.create(5000, ProviderCode.TMONEY, "+22890123456", "CMD-1");
        assertThatThrownBy(() -> p.complete(PaymentStatus.PENDING))
                .isInstanceOf(BusinessException.class);
    }
}
