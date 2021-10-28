package fr.dossierfacile.process.file.amqp;

import com.google.gson.Gson;
import fr.dossierfacile.process.file.amqp.model.TenantModel;
import fr.dossierfacile.process.file.service.interfaces.ProcessTenant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class ReceiverOcr {

    private final Gson gson;
    private final ProcessTenant processTenant;

    public void receiveMessage(String message) {
        log.info("Receive process file");
        TenantModel tenantModel = gson.fromJson(message, TenantModel.class);
        processTenant.process(tenantModel);
    }
}
