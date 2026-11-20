package tg.nexuspay.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Écrit dans la même transaction que l'opération : si l'opération échoue, la trace disparaît avec elle.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, UUID paymentId, String details) {
        repository.save(new AuditEvent(action, paymentId, details));
    }
}
