package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Message;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.dto.BrevoMailHistoryViewDTO;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.BrevoMailHistoryService;
import fr.gouv.bo.service.MessageService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BOMessageControllerTest {

    private TenantService tenantService;
    private MessageService messageService;
    private UserService userService;
    private BOApplicationAccessService applicationAccessService;
    private BrevoMailHistoryService brevoMailHistoryService;
    private BOMessageController controller;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        messageService = mock(MessageService.class);
        userService = mock(UserService.class);
        applicationAccessService = mock(BOApplicationAccessService.class);
        brevoMailHistoryService = mock(BrevoMailHistoryService.class);
        controller = new BOMessageController(
                tenantService,
                messageService,
                userService,
                applicationAccessService,
                brevoMailHistoryService
        );
    }

    @Test
    void tenantMessages_doesNotCallBrevoAndReturnsMessageView() {
        UserPrincipal principal = supportPrincipal();
        Tenant tenantUser = new Tenant();
        tenantUser.setId(10L);
        tenantUser.setEmail("tenant@example.com");
        Tenant tenant = new Tenant();
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(33L);
        tenant.setApartmentSharing(apartmentSharing);
        BOUser operator = new BOUser();
        operator.setEmail("support@test.com");

        when(tenantService.getUserById(10L)).thenReturn(tenantUser);
        when(tenantService.getTenantById(10L)).thenReturn(tenant);
        when(userService.findUserByEmail("support@test.com")).thenReturn(operator);
        when(messageService.findTenantMessages(tenantUser)).thenReturn(List.of(new Message()));

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.tenantMessages(model, 10L, principal);

        assertThat(view).isEqualTo("bo/message");
        assertThat(model.containsAttribute("brevoHistory")).isFalse();
        verify(brevoMailHistoryService, never()).getLast90DaysHistory(org.mockito.ArgumentMatchers.any());
    }

    @Nested
    class TenantBrevoHistory {

        private static final Long TENANT_ID = 10L;

        @Test
        void tenantBrevoHistory_populatesBrevoHistoryInModelAndReturnsFragment() {
            Tenant tenantUser = new Tenant();
            tenantUser.setId(TENANT_ID);
            tenantUser.setEmail("tenant@example.com");
            BrevoMailHistoryViewDTO history = BrevoMailHistoryViewDTO.builder().items(List.of()).build();

            when(tenantService.getUserById(TENANT_ID)).thenReturn(tenantUser);
            when(brevoMailHistoryService.getLast90DaysHistory("tenant@example.com")).thenReturn(history);

            ExtendedModelMap model = new ExtendedModelMap();
            String view = controller.tenantBrevoHistory(model, TENANT_ID, supportPrincipal());

            assertThat(view).isEqualTo("bo/fragments/brevo-mail-history :: brevo-history");
            assertThat(model.getAttribute("brevoHistory")).isEqualTo(history);
            assertThat(model.getAttribute("tenant")).isEqualTo(tenantUser);
        }

        @Test
        void tenantBrevoHistory_whenAccessServiceThrowsAccessDenied_propagatesException() {
            UserPrincipal principal = operatorPrincipal();
            doThrow(new AccessDeniedException("forbidden"))
                    .when(applicationAccessService)
                    .checkTenantAccess(any(), eq(TENANT_ID));

            assertThatThrownBy(() -> controller.tenantBrevoHistory(new ExtendedModelMap(), TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("forbidden");

            verify(brevoMailHistoryService, never()).getLast90DaysHistory(any());
        }

        @Test
        void tenantBrevoHistory_callsAccessCheckWithCorrectArguments() {
            UserPrincipal principal = supportPrincipal();
            Tenant tenantUser = new Tenant();
            tenantUser.setId(TENANT_ID);
            tenantUser.setEmail("tenant@example.com");
            when(tenantService.getUserById(TENANT_ID)).thenReturn(tenantUser);
            when(brevoMailHistoryService.getLast90DaysHistory(any())).thenReturn(
                    BrevoMailHistoryViewDTO.builder().items(List.of()).build());

            controller.tenantBrevoHistory(new ExtendedModelMap(), TENANT_ID, principal);

            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
        }
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(
                10L,
                "operator",
                "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(
                20L,
                "support",
                "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT"))
        );
    }
}
