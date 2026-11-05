package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.CompletedDossierService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TenantServiceReprocessTenantTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OPERATOR_ID = 20L;
    private static final String OPERATOR_EMAIL = "support@test.com";

    private TenantCommonRepository tenantRepository;
    private DocumentRepository documentRepository;
    private TenantLogService tenantLogService;
    private TenantLogCommonService tenantLogCommonService;
    private CompletedDossierService completedDossierService;
    private TenantService tenantService;

    private final UserPrincipal principal = new UserPrincipal(OPERATOR_ID, "support", OPERATOR_EMAIL,
            Set.of(new SimpleGrantedAuthority("ROLE_SUPPORT")));

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantCommonRepository.class);
        documentRepository = mock(DocumentRepository.class);
        tenantLogService = mock(TenantLogService.class);
        tenantLogCommonService = mock(TenantLogCommonService.class);
        completedDossierService = mock(CompletedDossierService.class);
        UserService userService = mock(UserService.class);
        when(userService.findUserByEmail(OPERATOR_EMAIL)).thenReturn(BOUser.builder().id(OPERATOR_ID).build());

        tenantService = new TenantService(
                tenantRepository,
                null, // mailService
                null, // partnerCallBackService
                userService,
                null, // messageSource
                documentRepository,
                null, // documentDeniedReasonsRepository
                null, // messageService
                null, // apartmentSharingRepository
                null, // operatorLogRepository
                null, // documentDeniedReasonsService
                null, // documentService
                tenantLogService,
                null, // keycloakService
                null, // apartmentSharingService
                null, // apartmentSharingLinkService
                null, // guarantorRepository
                null, // tenantMapperForMail
                null, // apartmentSharingMapperForMail
                null, // tenantCommonService
                tenantLogCommonService,
                null, // quotaService
                null, // sharedFileRepository
                completedDossierService,
                null, // operatorReviewPolicy
                null, // featureFlagService
                null // lotteryTicketService
        );
    }

    @Test
    void reprocessTenant_whenCompleted_sendsDossierToOperatorQueue() {
        // A COMPLETED dossier only holds TO_PROCESS documents
        Document document = Document.builder().id(10L).documentStatus(DocumentStatus.TO_PROCESS).build();
        Tenant tenant = tenant(TenantFileStatus.COMPLETED, document);

        tenantService.reprocessTenant(principal, TENANT_ID);

        verify(completedDossierService).forceOperatorReview(tenant, OPERATOR_ID);
        // The switch is fully delegated: no document change, no reprocess log
        verifyNoInteractions(documentRepository, tenantLogService, tenantLogCommonService);
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void reprocessTenant_whenDeclined_reprocessesDeclinedDocuments() {
        Document declined = Document.builder().id(10L).documentStatus(DocumentStatus.DECLINED).build();
        Document validated = Document.builder().id(11L).documentStatus(DocumentStatus.VALIDATED).build();
        Tenant tenant = tenant(TenantFileStatus.DECLINED, declined, validated);

        tenantService.reprocessTenant(principal, TENANT_ID);

        assertThat(declined.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
        assertThat(validated.getDocumentStatus()).isEqualTo(DocumentStatus.VALIDATED);
        verify(documentRepository).save(declined);
        verify(tenantRepository).save(tenant);
        verify(tenantLogService).addReprocessTenantLog(TENANT_ID, OPERATOR_ID, 1);
        verify(completedDossierService, never()).forceOperatorReview(any(), any());
    }

    @Test
    void reprocessTenant_whenNeitherCompletedNorDeclinedDocument_doesNothing() {
        Document document = Document.builder().id(10L).documentStatus(DocumentStatus.TO_PROCESS).build();
        tenant(TenantFileStatus.TO_PROCESS, document);

        tenantService.reprocessTenant(principal, TENANT_ID);

        verify(completedDossierService, never()).forceOperatorReview(any(), any());
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(tenantLogService, never()).addReprocessTenantLog(anyLong(), anyLong(), anyInt());
        verifyNoInteractions(documentRepository, tenantLogCommonService);
    }

    private Tenant tenant(TenantFileStatus status, Document... documents) {
        Tenant tenant = Tenant.builder()
                .id(TENANT_ID)
                .status(status)
                .honorDeclaration(false)
                .documents(new ArrayList<>(List.of(documents)))
                .guarantors(new ArrayList<>())
                .build();
        when(tenantRepository.findOneById(TENANT_ID)).thenReturn(tenant);
        return tenant;
    }
}
