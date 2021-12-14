package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.process.file.amqp.model.TenantModel;

public interface ProcessTenant {
    void process(TenantModel tenantModel);
}
