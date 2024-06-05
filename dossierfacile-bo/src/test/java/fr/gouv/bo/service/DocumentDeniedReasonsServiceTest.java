package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.gouv.bo.repository.DocumentDeniedReasonsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.enums.LogType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue,OptionalUsedAsFieldOrParameterType")
class DocumentDeniedReasonsServiceTest {

    private final DocumentDeniedReasonsRepository reasonsRepository = mock(DocumentDeniedReasonsRepository.class);
    private final TenantLogService logService = mock(TenantLogService.class);
    private final DocumentDeniedReasonsService service = new DocumentDeniedReasonsService(reasonsRepository, logService);

    @Nested
    class LastDeniedReasonSelection {

        private static final long DOCUMENT_ID = 11L;
        private static final long TENANT_ID = 22L;

        @Test
        void document_has_never_been_denied() {
            assertThat(getLastDeniedReasonOfDocument(DOCUMENT_ID, TENANT_ID)).isEmpty();
        }

        @Test
        void should_select_most_recent_reason() {
            LocalDateTime date = LocalDateTime.now();
            when(reasonsRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(List.of(
                    documentDeniedReasons(1L, date.minusDays(1)), // newest
                    documentDeniedReasons(2L, date.minusDays(2)) // oldest
            ));

            Optional<DocumentDeniedReasons> lastReason = getLastDeniedReasonOfDocument(DOCUMENT_ID, TENANT_ID);

            assertReasonIdEqualTo(1L, lastReason);
        }

        @Test
        void should_ignore_reasons_prior_to_last_validation() {
            LocalDateTime date = LocalDateTime.now();
            when(reasonsRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(List.of(
                    documentDeniedReasons(1L, date.minusDays(2)),
                    documentDeniedReasons(2L, date.minusDays(4))
            ));
            when(logService.getLogByTenantId(TENANT_ID)).thenReturn(List.of(
                    TenantLog.builder().creationDateTime(date.minusDays(1)).logType(ACCOUNT_VALIDATED).build(),
                    TenantLog.builder().creationDateTime(date.minusDays(3)).logType(ACCOUNT_VALIDATED).build()
            ));

            assertThat(getLastDeniedReasonOfDocument(DOCUMENT_ID, TENANT_ID)).isEmpty();
        }

        @Test
        void should_select_most_recent_reason_since_last_validation() {
            LocalDateTime date = LocalDateTime.now();
            when(reasonsRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(List.of(
                    documentDeniedReasons(1L, date.minusDays(1)),
                    documentDeniedReasons(2L, date.minusDays(2)),
                    documentDeniedReasons(3L, date.minusDays(3)),
                    documentDeniedReasons(4L, date.minusDays(4))
            ));
            when(logService.getLogByTenantId(TENANT_ID)).thenReturn(List.of(
                    TenantLog.builder().creationDateTime(date.minusDays(2)).logType(ACCOUNT_VALIDATED).build()
            ));

            Optional<DocumentDeniedReasons> lastReason = getLastDeniedReasonOfDocument(DOCUMENT_ID, TENANT_ID);

            assertReasonIdEqualTo(1L, lastReason);
        }

        @Test
        void should_ignore_other_types_of_log() {
            LocalDateTime date = LocalDateTime.now();
            when(reasonsRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(List.of(
                    documentDeniedReasons(1L, date)
            ));
            when(logService.getLogByTenantId(TENANT_ID)).thenReturn(List.of(
                    TenantLog.builder().creationDateTime(date.minusDays(1)).logType(ACCOUNT_DENIED).build(),
                    TenantLog.builder().creationDateTime(date.minusDays(2)).logType(ACCOUNT_COMPLETED).build(),
                    TenantLog.builder().creationDateTime(date.minusDays(3)).logType(ACCOUNT_EDITED).build()
            ));

            Optional<DocumentDeniedReasons> lastReason = getLastDeniedReasonOfDocument(DOCUMENT_ID, TENANT_ID);

            assertReasonIdEqualTo(1L, lastReason);
        }

        private DocumentDeniedReasons documentDeniedReasons(long id, LocalDateTime creationDate) {
            return DocumentDeniedReasons.builder()
                    .id(id)
                    .creationDate(creationDate)
                    .build();
        }

        private Optional<DocumentDeniedReasons> getLastDeniedReasonOfDocument(long documentId, long tenantId) {
            Document document = Document.builder().id(documentId).build();
            Tenant tenant = Tenant.builder().id(tenantId).build();
            return service.getLastDeniedReason(document, tenant);
        }

        private static void assertReasonIdEqualTo(long expectedId, Optional<DocumentDeniedReasons> lastReason) {
            assertThat(lastReason).isPresent();
            assertThat(lastReason.get().getId()).isEqualTo(expectedId);
        }

    }

}