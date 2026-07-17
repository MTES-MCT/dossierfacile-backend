package fr.gouv.bo.controller;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.application.exception.UnauthorizedException;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.gouv.bo.application.usecase.operator.OperatorDeleteFileUseCase;
import fr.gouv.bo.dto.CustomMessage;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.security.BOAccessDenied;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.BOTenantResolver;
import fr.gouv.bo.service.DocumentDeniedReasonsService;
import fr.gouv.bo.service.DocumentService;
import fr.gouv.bo.service.MessageService;
import fr.gouv.bo.service.TenantLogService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import fr.gouv.bo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOTenantControllerTest {

    private static final Long TENANT_ID = 42L;
    private static final Long APARTMENT_SHARING_ID = 99L;
    private static final Long DOCUMENT_ID = 5L;
    private static final Long FILE_ID = 12L;
    private static final Long GUARANTOR_ID = 8L;

    @Mock
    private TenantService tenantService;
    @Mock
    private MessageService messageService;
    @Mock
    private DocumentService documentService;
    @Mock
    private UserApiService userApiService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private UserService userService;
    @Mock
    private TenantLogService logService;
    @Mock
    private DocumentDeniedReasonsService documentDeniedReasonsService;
    @Mock
    private BOApplicationAccessService applicationAccessService;
    @Mock
    private BOTenantResolver tenantResolver;
    @Mock
    private fr.gouv.bo.application.usecase.operator.OperatorDeleteFileUseCase operatorDeleteFileUseCase;

    private BOTenantController controller;

    @BeforeEach
    void setUp() {
        controller = new BOTenantController(
                tenantService,
                messageService,
                documentService,
                userApiService,
                partnerCallBackService,
                userService,
                logService,
                documentDeniedReasonsService,
                applicationAccessService,
                tenantResolver,
                operatorDeleteFileUseCase
        );
    }

    @Nested
    class GetTenant {

        @Test
        void whenTenantDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(null);

            assertThatThrownBy(() -> controller.getTenant(TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(applicationAccessService, never()).checkTenantAccess(any(), any());
        }

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.getTenant(TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }

        @Test
        void whenOperatorAssigned_returnsRedirect() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);

            String view = controller.getTenant(TENANT_ID, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
        }
    }

    @Nested
    class DeleteDocument {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantResolver.resolveTenantFromDocument(DOCUMENT_ID)).thenReturn(accessTenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.deleteDocument(DOCUMENT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).deleteDocument(any(), any());
        }

        @Test
        void whenOperatorAssigned_deletesDocumentAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            BOUser operator = new BOUser();
            when(tenantResolver.resolveTenantFromDocument(DOCUMENT_ID)).thenReturn(accessTenant);
            when(userService.findUserByEmail(principal.getEmail())).thenReturn(operator);
            when(tenantService.deleteDocument(DOCUMENT_ID, operator)).thenReturn(accessTenant);

            String view = controller.deleteDocument(DOCUMENT_ID, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
            verify(tenantService).deleteDocument(DOCUMENT_ID, operator);
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void whenModelNotFound_returnsHomeRedirect() {
            UserPrincipal principal = operatorPrincipal();
            doThrow(new ModelNotFoundException(Operator.class, "Model not found"))
                    .when(operatorDeleteFileUseCase)
                    .execute(any(OperatorDeleteFileUseCase.OperatorDeleteFileCommand.class));

            String view = controller.deleteFile(FILE_ID, principal);

            assertThat(view).isEqualTo("redirect:/bo");
        }

        @Test
        void whenUnauthorized_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            doThrow(new UnauthorizedException("Unauthorized"))
                    .when(operatorDeleteFileUseCase)
                    .execute(any(OperatorDeleteFileUseCase.OperatorDeleteFileCommand.class));

            assertThatThrownBy(() -> controller.deleteFile(FILE_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }

        @Test
        void whenOperatorAssigned_deletesFileAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            var result = new OperatorDeleteFileUseCase.OperatorDeleteFileResult(APARTMENT_SHARING_ID, TENANT_ID);
            when(operatorDeleteFileUseCase.execute(new OperatorDeleteFileUseCase.OperatorDeleteFileCommand(FILE_ID, principal.getEmail())))
                    .thenReturn(result);

            String view = controller.deleteFile(FILE_ID, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
        }
    }

    @Nested
    class DeleteGuarantor {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantResolver.resolveTenantFromGuarantor(GUARANTOR_ID)).thenReturn(accessTenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.deleteGuarantor(GUARANTOR_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).deleteGuarantor(any(), any());
        }

        @Test
        void whenOperatorAssigned_deletesGuarantorAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            BOUser operator = new BOUser();
            when(tenantResolver.resolveTenantFromGuarantor(GUARANTOR_ID)).thenReturn(accessTenant);
            when(userService.findUserByEmail(principal.getEmail())).thenReturn(operator);
            when(tenantService.deleteGuarantor(GUARANTOR_ID, operator)).thenReturn(accessTenant);

            String view = controller.deleteGuarantor(GUARANTOR_ID, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
            verify(tenantService).deleteGuarantor(GUARANTOR_ID, operator);
        }
    }

    @Nested
    class ChangeStatusOfDocument {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantResolver.resolveTenantFromDocument(DOCUMENT_ID)).thenReturn(accessTenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.changeStatusOfDocument(DOCUMENT_ID, new MessageDTO(), principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).changeDocumentStatus(any(), any(), any());
        }

        @Test
        void whenOperatorAssigned_changesStatusAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            Tenant accessTenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            BOUser operator = new BOUser();
            MessageDTO messageDTO = MessageDTO.builder().message("ok").build();
            when(tenantResolver.resolveTenantFromDocument(DOCUMENT_ID)).thenReturn(accessTenant);
            when(userService.findUserByEmail(principal.getEmail())).thenReturn(operator);
            when(tenantService.changeDocumentStatus(DOCUMENT_ID, messageDTO, operator)).thenReturn(accessTenant);

            String view = controller.changeStatusOfDocument(DOCUMENT_ID, messageDTO, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
            verify(tenantService).changeDocumentStatus(DOCUMENT_ID, messageDTO, operator);
        }
    }

    @Nested
    class CustomEmail {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.customEmail(TENANT_ID, new CustomMessage(), principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).customMessage(any(), any(), any());
        }

        @Test
        void whenOperatorAssigned_delegatesToTenantService() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            CustomMessage customMessage = new CustomMessage();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            when(tenantService.customMessage(principal, TENANT_ID, customMessage))
                    .thenReturn("redirect:/bo/colocation/99#tenant42");

            String view = controller.customEmail(TENANT_ID, customMessage, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
        }
    }

    @Nested
    class ProcessFile {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.processFile(TENANT_ID, null, new CustomMessage(), principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).processFile(any(), any(), any());
        }

        @Test
        void whenOperatorAssigned_processesFileAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            CustomMessage customMessage = new CustomMessage();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            when(tenantService.findNextTenantInCouple(TENANT_ID)).thenReturn(Optional.empty());
            when(tenantService.redirectToApplication(principal, null)).thenReturn("redirect:/bo");

            String view = controller.processFile(TENANT_ID, null, customMessage, principal);

            assertThat(view).isEqualTo("redirect:/bo");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
            verify(tenantService).processFile(TENANT_ID, customMessage, principal);
        }
    }

    @Nested
    class AddOperatorComment {

        @Test
        void whenOperatorNotAssigned_throwsGenericAccessDenied() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);
            doThrow(BOAccessDenied.generic())
                    .when(applicationAccessService)
                    .checkTenantAccess(principal, TENANT_ID);

            assertThatThrownBy(() -> controller.addOperatorComment(TENANT_ID, "comment", null, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).addOperatorComment(any(), any(), any());
        }

        @Test
        void whenOperatorAssigned_addsCommentAndRedirects() {
            UserPrincipal principal = operatorPrincipal();
            Tenant tenant = tenant(TENANT_ID, APARTMENT_SHARING_ID);
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant);

            String view = controller.addOperatorComment(TENANT_ID, "comment", null, principal);

            assertThat(view).isEqualTo("redirect:/bo/colocation/99#tenant42");
            verify(applicationAccessService).checkTenantAccess(principal, TENANT_ID);
            verify(tenantService).addOperatorComment(principal, TENANT_ID, "comment");
        }
    }

    @Nested
    class SupportTenantActions {

        @Test
        void validateTenantFile_whenTenantDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = supportPrincipal();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(null);

            assertThatThrownBy(() -> controller.validateTenantFile(TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).validateTenantFile(any(UserPrincipal.class), any(Long.class));
        }

        @Test
        void declineTenantFile_whenTenantDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = supportPrincipal();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(null);

            assertThatThrownBy(() -> controller.declineTenantFile(TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).declineTenant(any(), any());
        }

        @Test
        void reprocessTenantFile_whenTenantDoesNotExist_throwsGenericAccessDenied() {
            UserPrincipal principal = supportPrincipal();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(null);

            assertThatThrownBy(() -> controller.reprocessTenantFile(TENANT_ID, principal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);

            verify(tenantService, never()).reprocessTenant(any(), any());
        }

        @Test
        void validateTenantFile_whenTenantExists_delegatesToService() {
            UserPrincipal principal = supportPrincipal();
            when(tenantService.findTenantById(TENANT_ID)).thenReturn(tenant(TENANT_ID, APARTMENT_SHARING_ID));

            ResponseEntity<Void> response = controller.validateTenantFile(TENANT_ID, principal);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(tenantService).validateTenantFile(principal, TENANT_ID);
        }
    }

    private Tenant tenant(Long tenantId, Long apartmentSharingId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTenantType(TenantType.CREATE);
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(apartmentSharingId);
        tenant.setApartmentSharing(apartmentSharing);
        return tenant;
    }

    private UserPrincipal operatorPrincipal() {
        return new UserPrincipal(10L, "operator", "operator@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    }

    private UserPrincipal supportPrincipal() {
        return new UserPrincipal(20L, "support", "support@test.com",
                Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));
    }
}
