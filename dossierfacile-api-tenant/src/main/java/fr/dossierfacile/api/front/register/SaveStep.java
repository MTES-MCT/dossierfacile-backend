package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.common.entity.Tenant;

public interface SaveStep<T> {
    TenantModel saveStep(Tenant tenant, T formStep);
}
