package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.Email;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ApplicationV1 implements SaveStep<ApplicationForm> {

    private Application application;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, ApplicationForm applicationForm) {
        List<Tenant> existingCoTenants = tenant.getApartmentSharing().getTenants()
                .stream()
                .filter(t -> !t.getId().equals(tenant.getId()))
                .collect(Collectors.toList());

        List<@Email String> invitedCoTenants = applicationForm.getCoTenantEmail();

        List<CoTenantForm> tenantToCreate = getTenantsToCreate(existingCoTenants, invitedCoTenants);
        List<Tenant> tenantToDelete = getTenantsToDelete(existingCoTenants, invitedCoTenants);

        return application.saveStep(tenant, applicationForm.getApplicationType(), tenantToDelete, tenantToCreate);
    }

    private static List<CoTenantForm> getTenantsToCreate(List<Tenant> existingCoTenants, List<@Email String> invitedCoTenants) {
        List<String> existingCoTenantsEmails = existingCoTenants.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
        Predicate<String> alreadyInvited = email -> !existingCoTenantsEmails.contains(email);

        return invitedCoTenants.stream()
                .filter(alreadyInvited)
                .map(email -> new CoTenantForm("", "", "", email))
                .collect(Collectors.toList());
    }

    private static List<Tenant> getTenantsToDelete(List<Tenant> existingCoTenants, List<@Email String> invitedCoTenants) {
        Predicate<Tenant> notInvited = tenant -> invitedCoTenants.stream().noneMatch(email -> email.equals(tenant.getEmail()));
        return existingCoTenants.stream()
                .filter(notInvited)
                .collect(Collectors.toList());
    }

}