package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.form.SubscriptionTenantForm;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.enums.StepRegister;
import fr.dossierfacile.common.entity.Tenant;

public interface TenantService {
    <T> TenantModel saveStepRegister(Tenant tenant, T formStep, StepRegister step);

    void subscribeTenant(String propertyToken, SubscriptionTenantForm subscriptionTenantForm, Tenant tenant);
}
