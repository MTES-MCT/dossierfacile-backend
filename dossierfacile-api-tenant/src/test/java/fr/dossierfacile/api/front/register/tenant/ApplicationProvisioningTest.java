package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.exception.CoTenantEmailAlreadyExistsException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.CoTenantForm;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.*;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationProvisioningTest {
    private static final String EMAIL = "invitee@example.com";
    @Mock
    TenantCommonRepository tenants;
    @Mock
    UserRoleService roles;
    @Mock
    TenantMapper mapper;
    @Mock
    MailService mail;
    @Mock
    PasswordRecoveryTokenService tokens;
    @Mock
    ApartmentSharingRepository sharings;
    @Mock
    LogService logs;
    @Mock
    KeycloakService keycloak;
    @Mock
    ApartmentSharingService sharingService;
    @Mock
    PartnerCallBackService callbacks;
    @Mock
    UserService userService;
    @Mock
    ClientAuthenticationFacade client;
    @Mock
    TenantStatusService statuses;
    @Mock
    LotteryTicketService lottery;
    @InjectMocks
    Application application;

    @ParameterizedTest
    @EnumSource(value = ApplicationType.class, names = {"COUPLE", "GROUP"})
    void shouldPersistValidatedInvitationBeforeMailAndPreservePartnerCallbacks(ApplicationType type) {
        var main = mainTenant(type);
        var partner = UserApi.builder().id(10L).build();
        main.setTenantsUserApi(List.of(TenantUserApi.builder().userApi(partner).build()));
        when(keycloak.createKeycloakUser(EMAIL)).thenReturn("new-id");
        var token = PasswordRecoveryToken.builder().token("invitation").build();
        when(tokens.create(any())).thenReturn(token);

        application.saveStep(main, type, List.of(), List.of(CoTenantForm.builder().email(EMAIL).build()));

        var invited = main.getApartmentSharing().getTenants().stream().filter(t -> t != main).findFirst().orElseThrow();
        assertEquals("new-id", invited.getKeycloakId());
        assertEquals(EMAIL, invited.getEmail());
        assertEquals(TenantType.JOIN, invited.getTenantType());
        assertEquals(type == ApplicationType.COUPLE, invited.getHonorDeclaration());
        var order = inOrder(tenants, roles, tokens, mail);
        order.verify(tenants).save(invited);
        order.verify(roles).createRole(invited);
        order.verify(tenants).save(invited);
        order.verify(tokens).create(invited);
        order.verify(mail).sendEmailForFlatmates(main, invited, token, type);
        verify(callbacks, atLeastOnce()).registerTenant(invited, partner);
    }

    @Test
    void shouldRejectAnExistingTenantBeforeIssuingInvitation() {
        var main = mainTenant(ApplicationType.GROUP);
        when(keycloak.createKeycloakUser(EMAIL)).thenReturn("used-id");
        when(tenants.findByKeycloakId("used-id")).thenReturn(Tenant.builder().id(2L).build());

        assertThrows(CoTenantEmailAlreadyExistsException.class, () -> application.saveStep(
                main, ApplicationType.GROUP, List.of(), List.of(CoTenantForm.builder().email(EMAIL).build())));

        verify(tenants).delete(any(Tenant.class));
        verifyNoInteractions(roles, tokens, mail, callbacks);
        assertEquals(1, main.getApartmentSharing().getTenants().size());
    }

    @Test
    void shouldStopNewInvitationWhenKeycloakFails() {
        var main = mainTenant(ApplicationType.COUPLE);
        when(keycloak.createKeycloakUser(EMAIL)).thenThrow(new IllegalStateException("identity mismatch"));

        assertThrows(IllegalStateException.class, () -> application.saveStep(
                main, ApplicationType.COUPLE, List.of(), List.of(CoTenantForm.builder().email(EMAIL).build())));

        verifyNoInteractions(roles, tokens, mail);
    }

    @Test
    void shouldStopNewInvitationWhenSavingTenantFails() {
        var main = mainTenant(ApplicationType.GROUP);
        when(tenants.save(any())).thenThrow(new DataIntegrityViolationException("binding conflict"));

        assertThrows(DataIntegrityViolationException.class, () -> application.saveStep(
                main, ApplicationType.GROUP, List.of(), List.of(CoTenantForm.builder().email(EMAIL).build())));

        verifyNoInteractions(roles, tokens, mail, callbacks);
    }

    @Test
    void shouldPreserveAddingEmailToAnExistingCotenant() {
        var main = mainTenant(ApplicationType.COUPLE);
        var invited = Tenant.builder().id(2L).build();
        when(keycloak.createKeycloakUser(EMAIL)).thenReturn("bound-id");

        application.linkEmailToTenants(main, List.of(Pair.of(invited, EMAIL)));

        assertEquals(EMAIL, invited.getEmail());
        assertEquals("bound-id", invited.getKeycloakId());
        var order = inOrder(roles, tenants, tokens, mail);
        order.verify(roles).createRole(invited);
        order.verify(tenants).save(invited);
        order.verify(tokens).create(invited);
        order.verify(mail).sendEmailForFlatmates(main, invited, null, ApplicationType.COUPLE);
    }

    @Test
    void shouldRejectAnExistingTenantWhenAddingEmail() {
        var invited = Tenant.builder().id(2L).build();
        when(keycloak.createKeycloakUser(EMAIL)).thenReturn("used-id");
        when(tenants.findByKeycloakId("used-id")).thenReturn(Tenant.builder().id(3L).build());

        assertThrows(CoTenantEmailAlreadyExistsException.class, () -> application.linkEmailToTenants(
                mainTenant(ApplicationType.GROUP), List.of(Pair.of(invited, EMAIL))));

        assertNull(invited.getKeycloakId());
        assertNull(invited.getEmail());
        verifyNoInteractions(roles, tokens, mail, callbacks);
        verify(tenants, never()).save(any());
    }

    @Test
    void shouldStopAddingEmailWhenKeycloakFails() {
        var invited = Tenant.builder().id(2L).keycloakId("old-id").build();
        when(keycloak.createKeycloakUser(EMAIL)).thenThrow(new IllegalStateException("identity mismatch"));

        assertThrows(IllegalStateException.class, () -> application.linkEmailToTenants(
                mainTenant(ApplicationType.GROUP), List.of(Pair.of(invited, EMAIL))));

        assertEquals("old-id", invited.getKeycloakId());
        assertNull(invited.getEmail());
        verifyNoInteractions(tenants, roles, tokens, mail, callbacks);
    }

    @Test
    void shouldStopAddingEmailWhenDatabaseRejectsBinding() {
        var invited = Tenant.builder().id(2L).build();
        when(keycloak.createKeycloakUser(EMAIL)).thenReturn("new-id");
        when(tenants.save(invited)).thenThrow(new DataIntegrityViolationException("binding conflict"));

        assertThrows(DataIntegrityViolationException.class, () -> application.linkEmailToTenants(
                mainTenant(ApplicationType.GROUP), List.of(Pair.of(invited, EMAIL))));

        verifyNoInteractions(tokens, mail, callbacks);
    }

    private Tenant mainTenant(ApplicationType type) {
        var sharing = ApartmentSharing.builder().applicationType(type).build();
        var main = Tenant.builder().id(1L).apartmentSharing(sharing).honorDeclaration(true).build();
        sharing.setTenants(new ArrayList<>(List.of(main)));
        return main;
    }
}
