package fr.dossierfacile.process.file.amqp;

import com.google.gson.Gson;
import fr.dossierfacile.process.file.amqp.model.TenantModel;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class Receiver {
    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final Gson gson;
    private final ProcessTenant processTenant;
    private final Producer producer;

    public void receiveMessage(String message) {
        try {
            log.info("Receive process file");
            TenantModel tenantModel = gson.fromJson(message, TenantModel.class);
            log.info("Tenant ID received [" + tenantModel.getId() + "]");
            try {
                processTenant.process(tenantModel.getId());
            } catch (Exception e) {
                log.error(EXCEPTION + Sentry.captureException(e));
                log.error(e.getMessage(), e.getCause());
                producer.processFile(tenantModel.getId());
            }
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }
}
