package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// deprecated since 20220909
@Deprecated
@Service
@AllArgsConstructor
public class ApplicationV1 implements SaveStep<ApplicationForm> {

    private Application application;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationForm applicationForm) {
        List<Tenant> oldCoTenant = tenant.getApartmentSharing().getTenants()
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .collect(Collectors.toList());

        List<Tenant> tenantToDelete = oldCoTenant.stream()
                .filter(t -> applicationForm.getCoTenantEmail().parallelStream()
                        .noneMatch(currentTenantEmail -> currentTenantEmail.equals(t.getEmail())))
                .collect(Collectors.toList());

        List<CoTenantForm> tenantToCreate = applicationForm.getCoTenantEmail().stream()
                .filter(currentTenantEmail -> oldCoTenant.parallelStream()
                        .noneMatch(oldTenant -> currentTenantEmail.equals(oldTenant.getEmail())))
                .map(email -> new CoTenantForm("", "", "", email))
                .collect(Collectors.toList());

        return application.saveStep(tenant, applicationForm.getApplicationType(), tenantToDelete, tenantToCreate);
    }
}
