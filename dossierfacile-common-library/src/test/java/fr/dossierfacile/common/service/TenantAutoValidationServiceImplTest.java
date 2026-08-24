package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.AutoValidationResultStatus;
import fr.dossierfacile.common.enums.DocumentAutoValidationReason;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAutoValidationServiceImplTest {

    @InjectMocks
    private TenantAutoValidationServiceImpl tenantAutoValidationService;

    @Mock
    private TenantCommonRepository tenantCommonRepository;
    @Mock
    private DocumentCommonRepository documentCommonRepository;
    @Mock
    private TenantCommonService tenantCommonService;
    @Mock
    private TenantLogCommonService tenantLogCommonService;

    @Test
    @DisplayName("Should return true when document subCategory is VISALE")
    void isEligibleForAutoValidation_visale_returnsTrue() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.VISALE)
                .build();

        boolean eligible = tenantAutoValidationService.isEligibleForAutoValidation(document);

        assertTrue(eligible);
    }

    @Test
    @DisplayName("Should return true when document subCategory is OWNER")
    void isEligibleForAutoValidation_owner_returnsTrue() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.OWNER)
                .build();

        boolean eligible = tenantAutoValidationService.isEligibleForAutoValidation(document);

        assertTrue(eligible);
    }

    @Test
    @DisplayName("Should return false when document subCategory is not VISALE")
    void isEligibleForAutoValidation_otherSubCategory_returnsFalse() {
        Document document = Document.builder()
                .documentSubCategory(DocumentSubCategory.SALARY)
                .build();

        boolean eligible = tenantAutoValidationService.isEligibleForAutoValidation(document);

        assertFalse(eligible);
    }

    @Test
    @DisplayName("Should return false when document or subCategory is null")
    void isEligibleForAutoValidation_nullDocument_returnsFalse() {
        assertFalse(tenantAutoValidationService.isEligibleForAutoValidation(null));
        assertFalse(tenantAutoValidationService.isEligibleForAutoValidation(Document.builder().build()));
    }

    @Test
    @DisplayName("Should delegate to repository findTenantsToAutoValidate")
    void listTenantsToAutoValidate_delegatesToRepository() {
        LocalDateTime maxDate = LocalDateTime.now().minusMinutes(30);
        List<Tenant> tenants = List.of(Tenant.builder().id(10L).build());
        when(tenantCommonRepository.findTenantsToAutoValidate(maxDate)).thenReturn(tenants);

        List<Tenant> result = tenantAutoValidationService.listTenantsToAutoValidate(maxDate);

        assertThat(result).hasSize(1);
        verify(tenantCommonRepository).findTenantsToAutoValidate(maxDate);
    }

    @Nested
    @DisplayName("processAutoValidationForTenant tests")
    class ProcessAutoValidationForTenantTests {

        @Test
        @DisplayName("Should return false when tenant is not found")
        void returnsFalse_whenTenantNotFound() {
            when(tenantCommonRepository.findById(999L)).thenReturn(java.util.Optional.empty());

            boolean result = tenantAutoValidationService.processAutoValidationForTenant(999L);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when tenant is not readyForAutoValidation")
        void returnsFalse_whenTenantNotReady() {
            Tenant tenant = Tenant.builder().id(1L).readyForAutoValidation(false).status(TenantFileStatus.TO_PROCESS).build();
            when(tenantCommonRepository.findById(1L)).thenReturn(java.util.Optional.of(tenant));

            boolean result = tenantAutoValidationService.processAutoValidationForTenant(1L);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should unflag readyForAutoValidation, record failure log with NO_DOCUMENTS status when no documents in TO_PROCESS")
        void unflagsAndSaves_whenValidationFails() {
            Tenant tenant = Tenant.builder().id(1L).readyForAutoValidation(true).status(TenantFileStatus.TO_PROCESS).build();
            when(tenantCommonRepository.findById(1L)).thenReturn(java.util.Optional.of(tenant));
            when(tenantCommonRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

            boolean result = tenantAutoValidationService.processAutoValidationForTenant(1L);

            assertFalse(result);
            assertThat(tenant.getReadyForAutoValidation()).isFalse();
            verify(tenantCommonRepository).save(tenant);

            ArgumentCaptor<TenantLog> logCaptor = ArgumentCaptor.forClass(TenantLog.class);
            verify(tenantLogCommonService).saveTenantLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getLogType()).isEqualTo(LogType.ACCOUNT_AUTO_VALIDATION_FAILED);
            assertThat(logCaptor.getValue().getLogDetails().get("status").asText()).isEqualTo(AutoValidationResultStatus.NO_DOCUMENTS.name());
        }

        @Test
        @DisplayName("Should validate documents and tenant when all TO_PROCESS documents have CHECKED report with only passed rules")
        void validatesTenantAndDocuments_whenReportIsCheckedWithPassedRules() {
            DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                    .analysisStatus(DocumentAnalysisStatus.CHECKED)
                    .passedRules(List.of(new DocumentAnalysisRule()))
                    .failedRules(List.of())
                    .inconclusiveRules(List.of())
                    .build();

            Document visaleDoc = Document.builder()
                    .id(10L)
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentCategory(DocumentCategory.FINANCIAL)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .documentCategoryStep(DocumentCategoryStep.UNDEFINED)
                    .documentAnalysisReport(report)
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .readyForAutoValidation(true)
                    .status(TenantFileStatus.TO_PROCESS)
                    .documents(List.of(visaleDoc))
                    .build();

            when(tenantCommonRepository.findById(1L)).thenReturn(java.util.Optional.of(tenant));
            when(tenantCommonRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

            boolean result = tenantAutoValidationService.processAutoValidationForTenant(1L);

            assertTrue(result);
            assertThat(visaleDoc.getDocumentStatus()).isEqualTo(DocumentStatus.VALIDATED);
            assertThat(tenant.getReadyForAutoValidation()).isFalse();
            verify(documentCommonRepository).save(visaleDoc);
            verify(tenantCommonService).changeTenantStatusToValidated(tenant);

            ArgumentCaptor<TenantLog> logCaptor = ArgumentCaptor.forClass(TenantLog.class);
            verify(tenantLogCommonService, atLeastOnce()).saveTenantLog(logCaptor.capture());

            List<TenantLog> savedLogs = logCaptor.getAllValues();
            assertThat(savedLogs).extracting(TenantLog::getLogType)
                    .contains(LogType.ACCOUNT_VALIDATED, LogType.ACCOUNT_AUTOMATICALLY_VALIDATED);

            TenantLog autoValLog = savedLogs.stream()
                    .filter(l -> l.getLogType() == LogType.ACCOUNT_AUTOMATICALLY_VALIDATED)
                    .findFirst()
                    .orElseThrow();

            assertThat(autoValLog.getLogDetails().get("status").asText()).isEqualTo(AutoValidationResultStatus.VALIDATED.name());
            assertThat(autoValLog.getLogDetails().get("documents").get(0).get("documentSubCategory").asText()).isEqualTo("VISALE");
            assertThat(autoValLog.getLogDetails().get("documents").get(0).get("reason").asText()).isEqualTo(DocumentAutoValidationReason.VALIDATED.name());
        }

        @Test
        @DisplayName("Should evaluate all documents and record FAILED status with individual document reasons")
        void fallbacks_whenReportHasFailedRules() {
            DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                    .analysisStatus(DocumentAnalysisStatus.CHECKED)
                    .passedRules(List.of(new DocumentAnalysisRule()))
                    .failedRules(List.of(new DocumentAnalysisRule()))
                    .inconclusiveRules(List.of())
                    .build();

            Document visaleDoc = Document.builder()
                    .id(10L)
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentCategory(DocumentCategory.FINANCIAL)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .documentAnalysisReport(report)
                    .build();

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .readyForAutoValidation(true)
                    .status(TenantFileStatus.TO_PROCESS)
                    .documents(List.of(visaleDoc))
                    .build();

            when(tenantCommonRepository.findById(1L)).thenReturn(java.util.Optional.of(tenant));
            when(tenantCommonRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

            boolean result = tenantAutoValidationService.processAutoValidationForTenant(1L);

            assertFalse(result);
            assertThat(visaleDoc.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
            assertThat(tenant.getReadyForAutoValidation()).isFalse();
            verify(tenantCommonRepository).save(tenant);

            ArgumentCaptor<TenantLog> logCaptor = ArgumentCaptor.forClass(TenantLog.class);
            verify(tenantLogCommonService).saveTenantLog(logCaptor.capture());
            assertThat(logCaptor.getValue().getLogType()).isEqualTo(LogType.ACCOUNT_AUTO_VALIDATION_FAILED);
            assertThat(logCaptor.getValue().getLogDetails().get("status").asText()).isEqualTo(AutoValidationResultStatus.FAILED.name());
            assertThat(logCaptor.getValue().getLogDetails().get("documents").get(0).get("reason").asText()).isEqualTo(DocumentAutoValidationReason.FAILED_RULES_PRESENT.name());
        }
    }

    @Nested
    @DisplayName("isTenantReadyForAutoValidation tests")
    class IsTenantReadyForAutoValidationTests {

        @Test
        @DisplayName("Should return true when all non-auto-validatable docs are VALIDATED and only VISALE is TO_PROCESS")
        void returnsTrue_whenOnlyVisaleIsToProcessAndOtherDocsAreValidated() {
            Document validatedDoc = Document.builder()
                    .documentStatus(DocumentStatus.VALIDATED)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(validatedDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertTrue(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when a non-auto-validatable doc (e.g. SALARY) is also TO_PROCESS")
        void returnsFalse_whenNonAutoValidatableDocIsToProcess() {
            Document salaryDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(salaryDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when a document is DECLINED")
        void returnsFalse_whenDocIsDeclined() {
            Document declinedDoc = Document.builder()
                    .documentStatus(DocumentStatus.DECLINED)
                    .documentSubCategory(DocumentSubCategory.SALARY)
                    .build();

            Document visaleDoc = Document.builder()
                    .documentStatus(DocumentStatus.TO_PROCESS)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(declinedDoc, visaleDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }

        @Test
        @DisplayName("Should return false when no document is TO_PROCESS (all VALIDATED)")
        void returnsFalse_whenNoDocIsToProcess() {
            Document validatedDoc = Document.builder()
                    .documentStatus(DocumentStatus.VALIDATED)
                    .documentSubCategory(DocumentSubCategory.VISALE)
                    .build();

            Tenant tenant = Mockito.spy(Tenant.builder()
                    .honorDeclaration(true)
                    .documents(List.of(validatedDoc))
                    .build());

            doReturn(true).when(tenant).isAllCategories();

            assertFalse(tenantAutoValidationService.isTenantReadyForAutoValidation(tenant));
        }
    }
}
