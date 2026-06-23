package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.LogType;
import org.junit.jupiter.api.Test;

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
