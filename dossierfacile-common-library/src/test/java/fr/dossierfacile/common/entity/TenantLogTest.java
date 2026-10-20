package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.model.log.UpdateMonthlySum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantLogTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_render_new_file_deleted_log_from_operator() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.FILE_DELETED)
                .operatorId(4L)
                .logDetails(details("fileId", 7, "documentId", 5))
                .build();

        assertThat(log.getTitle()).isEqualTo("FILE_DELETED by opId: 4");
    }

    @Test
    void should_render_new_document_added_log_from_tenant() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.DOCUMENT_ADDED)
                .logDetails(details("documentId", 5))
                .build();

        assertThat(log.getTitle()).isEqualTo("DOCUMENT_ADDED");
    }

    @Test
    void should_render_automatic_document_deletion_with_its_source() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.DOCUMENT_DELETED)
                .logDetails(details("documentId", 5, "source", "ASYNC_FAILED_PDF_GENERATION"))
                .build();

        assertThat(log.getTitle()).isEqualTo("DOCUMENT_DELETED (auto: ASYNC_FAILED_PDF_GENERATION)");
    }

    @Test
    void should_prefer_the_operator_over_the_source() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.DOCUMENT_DELETED)
                .operatorId(4L)
                .logDetails(details("documentId", 5, "source", "ASYNC_FAILED_PDF_GENERATION"))
                .build();

        assertThat(log.getTitle()).isEqualTo("DOCUMENT_DELETED by opId: 4");
    }

    @Test
    void should_ignore_the_source_of_other_log_types() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.QUEUE_ENTERED)
                .logDetails(details("source", "SUBMISSION"))
                .build();

        assertThat(log.getTitle()).isEqualTo("QUEUE_ENTERED");
    }

    @Test
    void should_render_legacy_file_deleted_log_from_operator() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .operatorId(4L)
                .logDetails(details("fileId", 7, "editionType", "DELETE"))
                .build();

        assertThat(log.getTitle()).isEqualTo("OPERATOR_EDITED by opId: 4 : FILE DELETED");
    }

    @Test
    void should_render_legacy_document_added_log_from_tenant() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .logDetails(details("editionType", "ADD"))
                .build();

        assertThat(log.getTitle()).isEqualTo("ACCOUNT_EDITED : DOCUMENT ADDED");
    }

    @Test
    void should_render_operator_financial_edition() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .operatorId(4L)
                .logDetails(details("newSum", 1500, "oldSum", 1000))
                .build();

        assertThat(log.getTitle()).isEqualTo("OPERATOR_EDITED_FINANCIAL by opId: 4");
    }

    @Test
    void should_render_the_correction_of_a_missing_amount() {
        Document document = Document.builder().id(5L).documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY).monthlySum(null).build();
        TenantLog log = TenantLog.builder().logType(LogType.ACCOUNT_EDITED).operatorId(4L)
                .creationDateTime(LocalDateTime.of(2026, 10, 19, 12, 0))
                .logDetails(MAPPER.valueToTree(UpdateMonthlySum.from(document, 1500))).build();

        assertThat(log.getTitle()).isEqualTo("OPERATOR_EDITED_FINANCIAL by opId: 4");
        assertThat(log.getSubtitle()).isEqualTo("FINANCIAL - SALARY - OLD AMOUNT: non renseigné - NEW AMOUNT: 1500");
        assertThat(log.getModificationText()).isEqualTo("SALARY - Modification du 19.10.26 - Ancien montant non renseigné - Nouveau montant 1500");
    }

    @Test
    void should_keep_zero_distinct_from_a_missing_amount() {
        TenantLog log = TenantLog.builder().logDetails(details("oldSum", 0, "newSum", 1500)).build();

        assertThat(log.getSubtitle()).isEqualTo(" - OLD AMOUNT: 0 - NEW AMOUNT: 1500");
    }

    @Test
    void should_render_plain_account_edited_without_edition_details() {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .logDetails(details("step", "DOCUMENT_IDENTIFICATION"))
                .build();

        assertThat(log.getTitle()).isEqualTo("ACCOUNT_EDITED");
    }

    private static ObjectNode details(Object... keyValues) {
        ObjectNode node = MAPPER.createObjectNode();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            if (value instanceof Integer intValue) {
                node.put(key, intValue);
            } else {
                node.put(key, (String) value);
            }
        }
        return node;
    }

}
