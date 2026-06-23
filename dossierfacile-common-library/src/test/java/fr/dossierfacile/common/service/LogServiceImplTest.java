package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static fr.dossierfacile.common.enums.ApplicationType.ALONE;
import static fr.dossierfacile.common.enums.ApplicationType.GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LogServiceImplTest {

    private final TenantLogRepository logRepository = mock(TenantLogRepository.class);
    private final LogService logService = new LogServiceImpl(logRepository, null, null, new ObjectMapper());

    @Test
    void should_save_added_log_for_tenant_document() {
        Document document = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(tenantWithId(2L))
                .build();

        logService.saveDocumentAddedLog(document, tenantWithId(1L));

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.DOCUMENT_ADDED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getLogDetails()).hasToString("""
                {"documentCategory":"IDENTIFICATION","documentSubCategory":"FRENCH_IDENTITY_CARD","tenantId":2}""");
    }

    @Test
    void should_save_deleted_log_for_guarantor_document() {
        Document document = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .guarantor(Guarantor.builder().id(3L).build())
                .build();

        logService.saveDocumentDeletedLog(document, tenantWithId(2L));

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.DOCUMENT_DELETED);
        assertThat(savedLog.getTenantId()).isEqualTo(2L);
        assertThat(savedLog.getLogDetails()).hasToString("""
                {"documentCategory":"FINANCIAL","documentSubCategory":"SALARY","guarantorId":3}""");
    }

    @Test
    void should_save_added_log_for_tenant_file() {
        Document document = Document.builder()
                .id(5L)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .tenant(tenantWithId(2L))
                .build();
        File file = File.builder()
                .id(7L)
                .document(document)
                .storageFile(StorageFile.builder().name("payslip.pdf").build())
                .build();

        logService.saveFileAddedLog(file, tenantWithId(1L));

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.FILE_ADDED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getLogDetails()).hasToString("""
                {"documentCategory":"FINANCIAL","documentSubCategory":"SALARY","tenantId":2,"documentId":5,"fileId":7,"fileName":"payslip.pdf"}""");
    }

    @Test
    void should_save_deleted_log_for_tenant_file() {
        Document document = Document.builder()
                .id(5L)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .tenant(tenantWithId(2L))
                .build();
        File file = File.builder()
                .id(7L)
                .document(document)
                .storageFile(StorageFile.builder().name("payslip.pdf").build())
                .build();

        logService.saveFileDeletedLog(file, tenantWithId(1L));

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.FILE_DELETED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getLogDetails()).hasToString("""
                {"documentCategory":"FINANCIAL","documentSubCategory":"SALARY","tenantId":2,"documentId":5,"fileId":7,"fileName":"payslip.pdf"}""");
    }

    @Test
    void should_save_revocation_log() {
        logService.savePartnerAccessRevocationLog(tenantWithId(1L), UserApi.builder().id(2L).build());

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.PARTNER_ACCESS_REVOKED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getUserApis()).containsExactly(2L);
    }

    @Test
    void should_application_type_change_log() {
        logService.saveApplicationTypeChangedLog(List.of(tenantWithId(1L)), ALONE, GROUP);

        TenantLog savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.APPLICATION_TYPE_CHANGED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getLogDetails()).hasToString("""
                {"oldType":"ALONE","newType":"GROUP"}""");
    }

    private static Tenant tenantWithId(long id) {
        return Tenant.builder().id(id).build();
    }

    private TenantLog getSavedLog() {
        ArgumentCaptor<TenantLog> logCaptor = ArgumentCaptor.forClass(TenantLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        return logCaptor.getAllValues().getFirst();
    }

}