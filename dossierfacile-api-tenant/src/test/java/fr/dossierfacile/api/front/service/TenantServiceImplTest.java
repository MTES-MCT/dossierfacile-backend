package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.TenantIllegalStateException;
import fr.dossierfacile.api.front.form.ShareFileByLinkForm;
import fr.dossierfacile.api.front.form.ShareFileByMailForm;
import fr.dossierfacile.api.front.register.RegisterFactory;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.KeycloakService;
import fr.dossierfacile.api.front.service.interfaces.MailService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.api.front.service.interfaces.UserApiService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.dossierfacile.common.service.interfaces.ConfirmationTokenService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private ApartmentSharingRepository apartmentSharingRepository;
    @Mock
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    @Mock
    private ConfirmationTokenService confirmationTokenService;
    @Mock
    private LogService logService;
    @Mock
    private MailService mailService;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private RegisterFactory registerFactory;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private UserApiService userApiService;
    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;
    @Mock
    private TenantMapperForMail tenantMapperForMail;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private OperatorReviewPolicy operatorReviewPolicy;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private LotteryTicketService lotteryTicketService;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private ApartmentSharing apartmentSharing;
    private Tenant tenantCreate;
    private Tenant tenantJoin;
    private Document documentOnCreate;
    private Document documentOnJoin;
    private DocumentAnalysisReport reportOnCreate;
    private DocumentAnalysisReport reportOnJoin;

    @BeforeEach
    void setUp() {
        apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(1L);
        apartmentSharing.setApplicationType(ApplicationType.COUPLE);
        apartmentSharing.setTenants(new ArrayList<>());

        tenantCreate = new Tenant();
        tenantCreate.setId(10L);
        tenantCreate.setTenantType(TenantType.CREATE);
        tenantCreate.setApartmentSharing(apartmentSharing);
        tenantCreate.setDocuments(new ArrayList<>());
        tenantCreate.setGuarantors(new ArrayList<>());

        tenantJoin = new Tenant();
        tenantJoin.setId(11L);
        tenantJoin.setTenantType(TenantType.JOIN);
        tenantJoin.setApartmentSharing(apartmentSharing);
        tenantJoin.setDocuments(new ArrayList<>());
        tenantJoin.setGuarantors(new ArrayList<>());

        apartmentSharing.getTenants().add(tenantCreate);
        apartmentSharing.getTenants().add(tenantJoin);

        documentOnCreate = new Document();
        documentOnCreate.setId(100L);
        reportOnCreate = new DocumentAnalysisReport();
        documentOnCreate.setDocumentAnalysisReport(reportOnCreate);
        tenantCreate.getDocuments().add(documentOnCreate);

        documentOnJoin = new Document();
        documentOnJoin.setId(101L);
        reportOnJoin = new DocumentAnalysisReport();
        documentOnJoin.setDocumentAnalysisReport(reportOnJoin);
        tenantJoin.getDocuments().add(documentOnJoin);
    }

    @Test
    void addCommentAnalysis_throwsDocumentNotFoundException() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(fr.dossierfacile.api.front.exception.DocumentNotFoundException.class, () ->
                tenantService.addCommentAnalysis(tenantCreate, 999L, "Comment")
        );

        verify(documentAnalysisReportRepository, never()).save(any());
    }

    @Test
    void addCommentAnalysis_throwsAccessDeniedException() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(documentOnCreate));
        when(documentService.hasPermissionOnDocument(documentOnCreate, tenantJoin)).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                tenantService.addCommentAnalysis(tenantJoin, 100L, "Sneaky comment")
        );

        verify(documentAnalysisReportRepository, never()).save(any());
    }

    @Test
    void addCommentAnalysis_throwsNotFoundException_whenNoReport() {
        Document noReportDoc = new Document();
        noReportDoc.setId(102L);
        when(documentRepository.findById(102L)).thenReturn(Optional.of(noReportDoc));
        when(documentService.hasPermissionOnDocument(noReportDoc, tenantCreate)).thenReturn(true);

        assertThrows(fr.dossierfacile.common.exceptions.NotFoundException.class, () ->
                tenantService.addCommentAnalysis(tenantCreate, 102L, "Where is the report?")
        );

        verify(documentAnalysisReportRepository, never()).save(any());
    }

    @Test
    void addCommentAnalysis_successForOwnDocument() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(documentOnCreate));
        when(documentService.hasPermissionOnDocument(documentOnCreate, tenantCreate)).thenReturn(true);

        tenantService.addCommentAnalysis(tenantCreate, 100L, "Own comment");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnCreate);
        assertEquals("Own comment", reportOnCreate.getComment());
    }

    @Test
    void addCommentAnalysis_successForCoupleSecondaryDocumentWhenPrimary() {
        when(documentRepository.findById(101L)).thenReturn(Optional.of(documentOnJoin));
        when(documentService.hasPermissionOnDocument(documentOnJoin, tenantCreate)).thenReturn(true);

        // Tenant CREATE can access Tenant JOIN's documents
        tenantService.addCommentAnalysis(tenantCreate, 101L, "Primary commenting secondary");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnJoin);
        assertEquals("Primary commenting secondary", reportOnJoin.getComment());
    }

    @Test
    void addCommentAnalysis_successForCouplePrimaryDocumentWhenSecondary() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(documentOnCreate));
        when(documentService.hasPermissionOnDocument(documentOnCreate, tenantJoin)).thenReturn(true);

        // Tenant JOIN should be able to access Tenant CREATE's documents.
        tenantService.addCommentAnalysis(tenantJoin, 100L, "Secondary commenting primary");

        verify(documentAnalysisReportRepository, times(1)).save(reportOnCreate);
        assertEquals("Secondary commenting primary", reportOnCreate.getComment());
    }

    private Tenant aloneTenantWithStatus(TenantFileStatus status) {
        ApartmentSharing sharing = new ApartmentSharing();
        sharing.setId(2L);
        sharing.setApplicationType(ApplicationType.ALONE);
        sharing.setTenants(new ArrayList<>());

        Tenant tenant = new Tenant();
        tenant.setId(20L);
        tenant.setTenantType(TenantType.CREATE);
        tenant.setStatus(status);
        tenant.setApartmentSharing(sharing);
        sharing.getTenants().add(tenant);
        return tenant;
    }

    @Test
    void createSharingLink_isAllowedForValidatedDossier() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.VALIDATED);
        ShareFileByLinkForm form = ShareFileByLinkForm.builder().title("Agence").fullData(true).daysValid(30).build();

        String url = tenantService.createSharingLink(tenant, form);

        assertTrue(url.startsWith("/file/"));
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
    }

    @Test
    void createSharingLink_isAllowedForCompletedDossier() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        ShareFileByLinkForm form = ShareFileByLinkForm.builder().title("Agence").fullData(false).daysValid(30).build();

        String url = tenantService.createSharingLink(tenant, form);

        assertTrue(url.startsWith("/public-file/"));
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
    }

    @Test
    void createSharingLink_isRefusedForNonCompletedOrValidatedDossier() {
        for (TenantFileStatus status : new TenantFileStatus[]{
                TenantFileStatus.TO_PROCESS, TenantFileStatus.INCOMPLETE, TenantFileStatus.DECLINED}) {
            Tenant tenant = aloneTenantWithStatus(status);
            ShareFileByLinkForm form = ShareFileByLinkForm.builder().title("Agence").daysValid(30).build();

            assertThrows(TenantIllegalStateException.class, () -> tenantService.createSharingLink(tenant, form));
        }
        verify(apartmentSharingLinkRepository, never()).save(any(ApartmentSharingLink.class));
    }

    @Test
    void sendFileByMail_isAllowedForCompletedDossier() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        when(apartmentSharingLinkRepository.findByApartmentSharingAndCreationDateIsAfterAndDeletedIsFalse(any(), any()))
                .thenReturn(new ArrayList<>());
        ShareFileByMailForm form = ShareFileByMailForm.builder()
                .email("owner@example.com").title("Agence").message("Bonjour").fullData(true).daysValid(7).build();

        tenantService.sendFileByMail(tenant, form);

        verify(mailService).sendFileByMail(startsWith("/file/"), eq("owner@example.com"), eq("Bonjour"), any(), any(), any());
        verify(apartmentSharingLinkRepository).save(any(ApartmentSharingLink.class));
    }

    @Test
    void sendFileByMail_isRefusedForNonCompletedOrValidatedDossier() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.TO_PROCESS);
        ShareFileByMailForm form = ShareFileByMailForm.builder()
                .email("owner@example.com").title("Agence").message("Bonjour").daysValid(7).build();

        assertThrows(TenantIllegalStateException.class, () -> tenantService.sendFileByMail(tenant, form));
        verifyNoInteractions(mailService);
    }

    @Test
    void updateValidationRequest_resetsFullPdf_whenDossierLeavesCompleted() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(tenantStatusService.updateTenantStatus(tenant)).thenAnswer(invocation -> {
            tenant.setStatus(TenantFileStatus.TO_PROCESS);
            return tenant;
        });
        TransactionSynchronizationManager.initSynchronization();

        try {
            tenantService.updateValidationRequest(tenant, true);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // The full PDF was rendered with the "non verified" design and must be dropped
        verify(apartmentSharingCommonService).resetDossierPdfGenerated(tenant.getApartmentSharing());
    }

    @Test
    void updateValidationRequest_keepsFullPdf_whenDossierStaysCompleted() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(tenantStatusService.updateTenantStatus(tenant)).thenReturn(tenant);

        tenantService.updateValidationRequest(tenant, false);

        verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
    }

    // On a reviewed dossier the choice is recorded without any immediate effect:
    // it only applies to the next re-submission
    @Test
    void updateValidationRequest_onValidatedDossier_persistsChoiceWithoutTouchingStatusOrPdf() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.VALIDATED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(tenantStatusService.updateTenantStatus(tenant)).thenReturn(tenant);

        Tenant updated = tenantService.updateValidationRequest(tenant, false);

        assertEquals(TenantFileStatus.VALIDATED, updated.getStatus());
        assertEquals(Boolean.FALSE, updated.getValidationRequested());
        verify(logService).saveLog(LogType.VALIDATION_DECLINED, tenant.getId());
        verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void updateValidationRequest_onDeclinedDossier_persistsChoiceWithoutTouchingStatus() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.DECLINED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(tenantStatusService.updateTenantStatus(tenant)).thenReturn(tenant);

        Tenant updated = tenantService.updateValidationRequest(tenant, true);

        assertEquals(TenantFileStatus.DECLINED, updated.getStatus());
        assertEquals(Boolean.TRUE, updated.getValidationRequested());
        verify(logService).saveLog(LogType.VALIDATION_REQUESTED, tenant.getId());
        verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
        verifyNoInteractions(mailService);
    }

    // Lottery mode: the click registers an application instead of entering the queue
    @Test
    void updateValidationRequest_lotteryMode_registersAnApplicationWithoutTouchingStatus() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);
        when(lotteryTicketService.getCooldownEndDate(tenant.getId())).thenReturn(Optional.empty());

        Tenant updated = tenantService.updateValidationRequest(tenant, true);

        assertEquals(TenantFileStatus.COMPLETED, updated.getStatus());
        assertEquals(Boolean.TRUE, updated.getValidationRequested());
        verify(lotteryTicketService).apply(tenant);
        verify(logService).saveLog(LogType.VALIDATION_REQUESTED, tenant.getId());
        // No queue entry, no queue position, no mail: everything happens at draw time
        verifyNoInteractions(tenantStatusService, mailService);
        verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
    }

    @Test
    void updateValidationRequest_lotteryMode_isRefusedDuringCooldown() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.COMPLETED);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);
        when(lotteryTicketService.getCooldownEndDate(tenant.getId()))
                .thenReturn(Optional.of(java.time.LocalDate.now().plusDays(2)));

        assertThrows(TenantIllegalStateException.class, () -> tenantService.updateValidationRequest(tenant, true));
        verify(lotteryTicketService, never()).apply(any());
    }

    @Test
    void updateValidationRequest_lotteryMode_cancellationWithdrawsTheEntryAndRecomputesStatus() {
        Tenant tenant = aloneTenantWithStatus(TenantFileStatus.TO_PROCESS);
        when(operatorReviewPolicy.canRequestOperatorReview(tenant)).thenReturn(true);
        when(featureFlagService.isFeatureEnabled(LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG)).thenReturn(true);
        // Without a DRAWN ticket, the recomputation brings the drawn
        // unprocessed dossier back to COMPLETED
        when(tenantStatusService.updateTenantStatus(tenant)).thenAnswer(invocation -> {
            tenant.setStatus(TenantFileStatus.COMPLETED);
            return tenant;
        });

        Tenant updated = tenantService.updateValidationRequest(tenant, false);

        assertEquals(TenantFileStatus.COMPLETED, updated.getStatus());
        assertEquals(Boolean.FALSE, updated.getValidationRequested());
        verify(lotteryTicketService).cancelActiveTicket(tenant);
        verify(logService).saveLog(LogType.VALIDATION_DECLINED, tenant.getId());
        verifyNoInteractions(mailService);
    }

}
