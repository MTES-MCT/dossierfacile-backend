package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.ApplicationFormV2;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.PasswordRecoveryTokenService;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ExtendWith(MockitoExtension.class)
public class ApplicationTest {

    @Mock
    ApartmentSharingService apartmentSharingService;
    @Mock
    PartnerCallBackService partnerCallBackService;
    @Mock
    KeycloakService keycloakService;
    @Mock
    TenantCommonRepository tenantCommonRepository;
    @Mock
    ApartmentSharingRepository apartmentSharingRepository;
    @Mock
    UserService userService;
    @Mock
    UserRoleService userRoleService;
    @Mock
    PasswordRecoveryTokenService passwordRecoveryTokenService;
    @Mock
    MailService mailService;
    @Mock
    LogService logService;
    @Mock
    TenantMapper tenantMapper;
    @Mock
    ClientAuthenticationFacade clientAuthenticationFacade;

    @Spy
    @InjectMocks
    private Application application;

    @Test
    void process_whenTenantIsSame() {
        Tenant dbCoTenant = Tenant.builder().id(2L).firstName("first").lastName("last").build();
        Tenant dbMainTenant = Tenant.builder().apartmentSharing(
                ApartmentSharing.builder().tenants(Collections.singletonList(dbCoTenant)).build()
        ).build();

        ApplicationFormV2 form = ApplicationFormV2.builder().applicationType(ApplicationType.COUPLE)
                .coTenants(Collections.singletonList(CoTenantForm.builder().firstName("first").lastName("last").build())).build();
        application.saveStep(dbMainTenant, form);

        Mockito.verify(application).saveStep(dbMainTenant, form.getApplicationType(), Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void process_whenTenantIsSameWithMail() {
        Tenant dbCoTenant = Tenant.builder().id(2L).firstName("first").lastName("last").email("email@email").build();
        Tenant dbMainTenant = Tenant.builder().apartmentSharing(
                ApartmentSharing.builder().tenants(new ArrayList<>(Collections.singletonList(dbCoTenant))).build()
        ).build();

        ApplicationFormV2 form = ApplicationFormV2.builder().applicationType(ApplicationType.COUPLE)
                .coTenants(Collections.singletonList(CoTenantForm.builder().firstName("first").lastName("last").email("email@email").build())).build();
        application.saveStep(dbMainTenant, form);

        Mockito.verify(application).saveStep(dbMainTenant, form.getApplicationType(), Collections.emptyList(), Collections.emptyList());
    }

    @Test
    void process_whenTenantIsSameWithDifferentEMail() {
        Tenant dbCoTenant = Tenant.builder().id(2L).firstName("first").lastName("last").email("email@email").build();
        Tenant dbMainTenant = Tenant.builder().apartmentSharing(
                ApartmentSharing.builder().tenants(new ArrayList<>(Collections.singletonList((dbCoTenant)))).build()
        ).build();

        ApplicationFormV2 form = ApplicationFormV2.builder().applicationType(ApplicationType.COUPLE)
                .coTenants(Collections.singletonList(CoTenantForm.builder().firstName("first").lastName("last").email("other@email").build())).build();
        application.saveStep(dbMainTenant, form);

        Mockito.verify(application).saveStep(dbMainTenant, form.getApplicationType(),
                Collections.singletonList(dbCoTenant),
                Collections.singletonList(CoTenantForm.builder().firstName("first").lastName("last").email("other@email").build()));
        Mockito.verify(application).linkEmailToTenants(dbMainTenant, Collections.emptyList());
    }

    @Test
    void process_whenTenantIsSameWithNewEMailPreviouslyNULL() {
        Tenant dbCoTenant = Tenant.builder().id(2L).firstName("first").lastName("last").build();
        Tenant dbMainTenant = Tenant.builder().apartmentSharing(
                ApartmentSharing.builder().tenants(new ArrayList<>(Collections.singletonList((dbCoTenant)))).build()
        ).build();

        ApplicationFormV2 form = ApplicationFormV2.builder().applicationType(ApplicationType.COUPLE)
                .coTenants(Collections.singletonList(CoTenantForm.builder().firstName("first").lastName("last").email("new@email").build())).build();
        application.saveStep(dbMainTenant, form);

        Mockito.verify(application).saveStep(dbMainTenant, form.getApplicationType(),
                Collections.emptyList(), Collections.emptyList());
        Mockito.verify(application).linkEmailToTenants(dbMainTenant, List.of(new ImmutablePair<>(dbCoTenant, "new@email")));
    }

    @Test
    void process_whenTenantIsNotTheSame() {
        Tenant dbCoTenant = Tenant.builder().id(2L).firstName("first").lastName("last").build();
        Tenant dbMainTenant = Tenant.builder().apartmentSharing(
                ApartmentSharing.builder().tenants(new ArrayList<>(Collections.singletonList((dbCoTenant)))).build()
        ).build();

        CoTenantForm newCoTenantForm = CoTenantForm.builder().firstName("notfirst").lastName("last").build();
        ApplicationFormV2 form = ApplicationFormV2.builder().applicationType(ApplicationType.COUPLE)
                .coTenants(new ArrayList<>(Collections.singletonList(newCoTenantForm))).build();
        application.saveStep(dbMainTenant, form);

        Mockito.verify(application).saveStep(dbMainTenant, form.getApplicationType(),
                Collections.singletonList(dbCoTenant), Collections.singletonList(newCoTenantForm));
    }
}
