package fr.dossierfacile.scheduler.tasks.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsTableMappingTest {

    @Test
    @DisplayName("Devrait contenir exactement les 25 tables répliquées")
    void should_contain_all_25_tables() {
        assertThat(AnalyticsTableMapping.values()).hasSize(25);

        List<String> expectedDestTables = List.of(
                "apartment_sharing",
                "apartment_sharing_link",
                "document",
                "document_analysis_report",
                "document_denied_options",
                "document_denied_reasons",
                "feature_flag",
                "file",
                "guarantor",
                "operator_log",
                "link_log",
                "owner_log",
                "property",
                "property_log",
                "tenant",
                "tenant_log",
                "tenant_userapi",
                "user_account",
                "user_api",
                "user_feature_assignment",
                "user_feature_assignment_history",
                "watermark_document",
                "lottery_draw",
                "lottery_ticket",
                "user_operator"
        );

        List<String> actualDestTables = Arrays.stream(AnalyticsTableMapping.values())
                .map(AnalyticsTableMapping::getDestTableName)
                .toList();

        assertThat(actualDestTables).containsExactlyElementsOf(expectedDestTables);
    }

    @Test
    @DisplayName("Vérification RGPD stricte : aucune colonne sensible exclue ne doit être présente")
    void should_respect_strict_gdpr_exclusions() {
        // 1. guarantor : exclus first_name, last_name, legal_person_name, preferred_name, email
        AnalyticsTableMapping guarantor = AnalyticsTableMapping.GUARANTOR;
        assertThat(guarantor.getColumns()).containsExactly("id", "tenant_id", "type_guarantor");

        // 2. tenant : exclus first_name, last_name, preferred_name, beneficiary_email, clarification, operator_comment
        AnalyticsTableMapping tenant = AnalyticsTableMapping.TENANT;
        assertThat(tenant.getColumns()).noneMatch(col ->
                col.contains("tenant_first_name") ||
                col.contains("tenant_last_name") ||
                col.contains("tenant_preferred_name") ||
                col.contains("beneficiary_email") ||
                col.contains("clarification") ||
                col.contains("operator_comment")
        );

        // 3. operator_log : exclu metadata
        AnalyticsTableMapping operatorLog = AnalyticsTableMapping.OPERATOR_LOG;
        assertThat(operatorLog.getColumns()).noneMatch(col -> col.equalsIgnoreCase("metadata"));

        // 4. link_log : exclu ip_address
        AnalyticsTableMapping linkLog = AnalyticsTableMapping.LINK_LOG;
        assertThat(linkLog.getColumns()).noneMatch(col -> col.equalsIgnoreCase("ip_address"));

        // 5. owner_log : exclu json_profile
        AnalyticsTableMapping ownerLog = AnalyticsTableMapping.OWNER_LOG;
        assertThat(ownerLog.getColumns()).noneMatch(col -> col.equalsIgnoreCase("json_profile"));

        // 6. tenant_log : exclu user_apis
        AnalyticsTableMapping tenantLog = AnalyticsTableMapping.TENANT_LOG;
        assertThat(tenantLog.getColumns()).noneMatch(col -> col.equalsIgnoreCase("user_apis"));

        // 7. property : exclus address, ademe_number, ademe_api_result
        AnalyticsTableMapping property = AnalyticsTableMapping.PROPERTY;
        assertThat(property.getColumns()).noneMatch(col ->
                col.equalsIgnoreCase("address") ||
                col.equalsIgnoreCase("ademe_number") ||
                col.equalsIgnoreCase("ademe_api_result")
        );

        // 8. user_account : exclus email, first_name, last_name, preferred_name, keycloak_id, image_url, france_connect_*
        AnalyticsTableMapping userAccount = AnalyticsTableMapping.USER_ACCOUNT;
        assertThat(userAccount.getColumns()).noneMatch(col ->
                col.equalsIgnoreCase("email") ||
                col.equalsIgnoreCase("first_name") ||
                col.equalsIgnoreCase("last_name") ||
                col.equalsIgnoreCase("preferred_name") ||
                col.equalsIgnoreCase("keycloak_id") ||
                col.equalsIgnoreCase("image_url") ||
                col.startsWith("france_connect_")
        );

        // 9. user_api : exclus email, url_callback, partner_api_key_callback
        AnalyticsTableMapping userApi = AnalyticsTableMapping.USER_API;
        assertThat(userApi.getColumns()).noneMatch(col ->
                col.equalsIgnoreCase("email") ||
                col.equalsIgnoreCase("url_callback") ||
                col.equalsIgnoreCase("partner_api_key_callback")
        );

        // 10. watermark_document : exclu text
        AnalyticsTableMapping watermarkDoc = AnalyticsTableMapping.WATERMARK_DOCUMENT;
        assertThat(watermarkDoc.getColumns()).noneMatch(col -> col.equalsIgnoreCase("text"));

        // 11. document : exclu custom_text
        AnalyticsTableMapping doc = AnalyticsTableMapping.DOCUMENT;
        assertThat(doc.getColumns()).noneMatch(col -> col.equalsIgnoreCase("custom_text"));

        // 12. apartment_sharing_link : exclus email, title
        AnalyticsTableMapping link = AnalyticsTableMapping.APARTMENT_SHARING_LINK;
        assertThat(link.getColumns()).noneMatch(col ->
                col.equalsIgnoreCase("email") ||
                col.equalsIgnoreCase("title")
        );

        // 13. document_analysis_report : exclu comment
        AnalyticsTableMapping report = AnalyticsTableMapping.DOCUMENT_ANALYSIS_REPORT;
        assertThat(report.getColumns()).noneMatch(col -> col.equalsIgnoreCase("comment"));

        // 14. document_denied_reasons : exclu comment
        AnalyticsTableMapping reasons = AnalyticsTableMapping.DOCUMENT_DENIED_REASONS;
        assertThat(reasons.getColumns()).noneMatch(col -> col.equalsIgnoreCase("comment"));
    }

    @Test
    @DisplayName("Devrait hasher avec le sel secret les tokens et données d'identification")
    void should_hash_tokens_with_salt() {
        String salt = "secret_salt_123";

        String queryApartmentSharing = AnalyticsTableMapping.APARTMENT_SHARING.buildSelectQuery(salt);
        assertThat(queryApartmentSharing)
                .contains("encode(sha256((token || 'secret_salt_123')::bytea), 'hex') AS token")
                .contains("encode(sha256((token_public || 'secret_salt_123')::bytea), 'hex') AS token_public");

        String queryLink = AnalyticsTableMapping.APARTMENT_SHARING_LINK.buildSelectQuery(salt);
        assertThat(queryLink)
                .contains("encode(sha256((token::text || 'secret_salt_123')::bytea), 'hex') AS token");

        String queryDoc = AnalyticsTableMapping.DOCUMENT.buildSelectQuery(salt);
        assertThat(queryDoc)
                .contains("encode(sha256((name || 'secret_salt_123')::bytea), 'hex') AS name");

        String queryProperty = AnalyticsTableMapping.PROPERTY.buildSelectQuery(salt);
        assertThat(queryProperty)
                .contains("encode(sha256((token || 'secret_salt_123')::bytea), 'hex') AS token")
                .contains("encode(sha256((name || 'secret_salt_123')::bytea), 'hex') AS name");
    }

    @Test
    @DisplayName("Devrait générer la requête correcte pour user_operator")
    void should_generate_correct_query_for_user_operator() {
        AnalyticsTableMapping mapping = AnalyticsTableMapping.USER_OPERATOR;
        assertThat(mapping.getDestTableName()).isEqualTo("user_operator");
        assertThat(mapping.getSourceTableName()).isEqualTo("user_account");
        assertThat(mapping.getWhereClause()).isEqualTo("WHERE user_type = 'BO'");

        String query = mapping.buildSelectQuery("dummy_salt");
        assertThat(query)
                .startsWith("SELECT id, creation_date AS created_at, email, first_name, last_name, last_login_date, update_date_time AS updated_at FROM user_account WHERE user_type = 'BO'");
    }

    @Test
    @DisplayName("Devrait échapper les simples quotes dans le sel pour éviter l'injection SQL")
    void should_escape_single_quotes_in_salt() {
        String saltWithQuote = "salt'with'quotes";
        String query = AnalyticsTableMapping.APARTMENT_SHARING.buildSelectQuery(saltWithQuote);
        assertThat(query).contains("'salt''with''quotes'");
    }
}
