package fr.dossierfacile.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.LogRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LogServiceImplTest {

    private final LogRepository logRepository = mock(LogRepository.class);
    private final LogService logService = new LogServiceImpl(logRepository, null, new ObjectMapper());

    @Test
    void should_save_edition_log_for_tenant_document() {
        Document document = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.FRENCH_IDENTITY_CARD)
                .tenant(tenantWithId(2L))
                .build();

        logService.saveDocumentEditedLog(document, tenantWithId(1L));

        Log savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.ACCOUNT_EDITED);
        assertThat(savedLog.getTenantId()).isEqualTo(1L);
        assertThat(savedLog.getLogDetails()).isEqualTo("""
                {"documentCategory":"IDENTIFICATION","documentSubCategory":"FRENCH_IDENTITY_CARD","tenantId":2}""");
    }

    @Test
    void should_save_edition_log_for_guarantor_document() {
        Document document = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .noDocument(true)
                .guarantor(Guarantor.builder().id(3L).build())
                .build();

        logService.saveDocumentEditedLog(document, tenantWithId(2L));

        Log savedLog = getSavedLog();
        assertThat(savedLog.getLogType()).isEqualTo(LogType.ACCOUNT_EDITED);
        assertThat(savedLog.getTenantId()).isEqualTo(2L);
        assertThat(savedLog.getLogDetails()).isEqualTo("""
                {"documentCategory":"FINANCIAL","documentSubCategory":"SALARY","noDocument":true,"guarantorId":3}""");
    }

    private static Tenant tenantWithId(long id) {
        return Tenant.builder().id(id).build();
    }

    private Log getSavedLog() {
        ArgumentCaptor<Log> logCaptor = ArgumentCaptor.forClass(Log.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        return logCaptor.getAllValues().get(0);
    }

}