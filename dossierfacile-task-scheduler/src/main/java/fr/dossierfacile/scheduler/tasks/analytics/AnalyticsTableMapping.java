package fr.dossierfacile.scheduler.tasks.analytics;

import lombok.Getter;

import java.util.List;

@Getter
@SuppressWarnings("java:S1192") // Column names and SQL expressions intentionally kept inline for schema readability
public enum AnalyticsTableMapping {

    APARTMENT_SHARING(
            "apartment_sharing",
            "apartment_sharing",
            null,
            List.of(
                    "id",
                    "operator_date",
                    "application_type",
                    "dossier_pdf_document_status",
                    "last_update_date",
                    "pdf_dossier_file_id",
                    "encode(sha256((token || ':salt')::bytea), 'hex') AS token",
                    "encode(sha256((token_public || ':salt')::bytea), 'hex') AS token_public"
            )
    ),

    APARTMENT_SHARING_LINK(
            "apartment_sharing_link",
            "apartment_sharing_link",
            null,
            List.of(
                    "id",
                    "creation_date",
                    "apartment_sharing_id",
                    "full_data",
                    "disabled",
                    "link_type",
                    "last_sent_datetime",
                    "expiration_date",
                    "deleted",
                    "created_by",
                    "partner_id",
                    "property_id",
                    "failed_attempt_count",
                    "first_failed_attempt_at",
                    "encode(sha256((token::text || ':salt')::bytea), 'hex') AS token"
            )
    ),

    DOCUMENT(
            "document",
            "document",
            null,
            List.of(
                    "id",
                    "document_category",
                    "tenant_id",
                    "monthly_sum",
                    "guarantor_id",
                    "document_sub_category",
                    "document_status",
                    "no_document",
                    "creation_date",
                    "document_denied_reasons_id",
                    "avis_detected",
                    "watermark_file_id",
                    "last_modified_date",
                    "document_category_step",
                    "encode(sha256((name || ':salt')::bytea), 'hex') AS name"
            )
    ),

    DOCUMENT_ANALYSIS_REPORT(
            "document_analysis_report",
            "document_analysis_report",
            null,
            List.of(
                    "id",
                    "document_id",
                    "analysis_status",
                    "failed_rules",
                    "passed_rules",
                    "inconclusive_rules",
                    "created_at",
                    "data_document_id"
            )
    ),

    DOCUMENT_DENIED_OPTIONS(
            "document_denied_options",
            "document_denied_options",
            null,
            List.of(
                    "id",
                    "message_value",
                    "document_sub_category",
                    "document_user_type",
                    "code",
                    "document_category"
            )
    ),

    DOCUMENT_DENIED_REASONS(
            "document_denied_reasons",
            "document_denied_reasons",
            null,
            List.of(
                    "id",
                    "checked_options",
                    "message_id",
                    "checked_options_id",
                    "message_data",
                    "document_id",
                    "creation_date",
                    "document_category",
                    "document_sub_category",
                    "document_category_step",
                    "document_tenant_type"
            )
    ),

    FEATURE_FLAG(
            "feature_flag",
            "feature_flag",
            null,
            List.of(
                    "key",
                    "description",
                    "active",
                    "only_for_new_user",
                    "rollout_pct",
                    "deployment_date",
                    "created_at",
                    "updated_at"
            )
    ),

    FILE(
            "file",
            "file",
            null,
            List.of(
                    "id",
                    "document_id",
                    "creation_date",
                    "number_of_pages",
                    "preview_file_id",
                    "storage_file_id"
            )
    ),

    GUARANTOR(
            "guarantor",
            "guarantor",
            null,
            List.of(
                    "id",
                    "tenant_id",
                    "type_guarantor"
            )
    ),

    OPERATOR_LOG(
            "operator_log",
            "operator_log",
            null,
            List.of(
                    "id",
                    "tenant_id",
                    "operator_id",
                    "tenant_status",
                    "action_type",
                    "creation_date",
                    "processed_documents",
                    "time_spent"
            )
    ),

    LINK_LOG(
            "link_log",
            "link_log",
            null,
            List.of(
                    "id",
                    "apartment_sharing_id",
                    "link_type",
                    "creation_date",
                    "encode(sha256((token::text || ':salt')::bytea), 'hex') AS token"
            )
    ),

    OWNER_LOG(
            "owner_log",
            "owner_log",
            null,
            List.of(
                    "id",
                    "owner_id",
                    "creation_date",
                    "log_type"
            )
    ),

    PROPERTY(
            "property",
            "property",
            null,
            List.of(
                    "id",
                    "owner_id",
                    "creation_date",
                    "count_visit",
                    "property_id",
                    "rent_cost",
                    "displayed",
                    "notification",
                    "cant_email_sent_prospect",
                    "validated",
                    "type",
                    "furniture",
                    "charges_cost",
                    "living_space",
                    "energy_consumption",
                    "co2emission",
                    "validated_date",
                    "dpe_date",
                    "dpe_not_required",
                    "encode(sha256((token || ':salt')::bytea), 'hex') AS token",
                    "encode(sha256((name || ':salt')::bytea), 'hex') AS name"
            )
    ),

    PROPERTY_LOG(
            "property_log",
            "property_log",
            null,
            List.of(
                    "id",
                    "property_id",
                    "creation_date",
                    "log_type",
                    "apartment_sharing_id"
            )
    ),

    TENANT(
            "tenant",
            "tenant",
            null,
            List.of(
                    "id",
                    "tenant_type",
                    "apartment_sharing_id",
                    "satisfaction_survey",
                    "accept_access",
                    "zip_code",
                    "honor_declaration",
                    "last_update_date",
                    "status",
                    "operator_date_time",
                    "warnings",
                    "abroad",
                    "owner_type",
                    "ready_for_auto_validation",
                    "validation_requested"
            )
    ),

    TENANT_LOG(
            "tenant_log",
            "tenant_log",
            null,
            List.of(
                    "id",
                    "tenant_id",
                    "operator_id",
                    "log_type",
                    "creation_date",
                    "message_id",
                    "log_details"
            )
    ),

    TENANT_USERAPI(
            "tenant_userapi",
            "tenant_userapi",
            null,
            List.of(
                    "tenant_id",
                    "userapi_id",
                    "access_granted_date"
            )
    ),

    USER_ACCOUNT(
            "user_account",
            "user_account",
            null,
            List.of(
                    "id",
                    "creation_date",
                    "last_login_date",
                    "update_date_time",
                    "enabled",
                    "france_connect",
                    "user_type",
                    "acquisition_campaign",
                    "acquisition_source",
                    "acquisition_medium"
            )
    ),

    USER_API(
            "user_api",
            "user_api",
            null,
            List.of(
                    "id",
                    "name",
                    "name2",
                    "site",
                    "disabled",
                    "logo_url",
                    "completed_status_supported"
            )
    ),

    USER_FEATURE_ASSIGNMENT(
            "user_feature_assignment",
            "user_feature_assignment",
            null,
            List.of(
                    "user_id",
                    "feature_key",
                    "enabled",
                    "bucket",
                    "rollout_pct",
                    "assigned_at",
                    "assignment_source"
            )
    ),

    USER_FEATURE_ASSIGNMENT_HISTORY(
            "user_feature_assignment_history",
            "user_feature_assignment_history",
            null,
            List.of(
                    "id",
                    "user_id",
                    "feature_key",
                    "enabled",
                    "bucket",
                    "rollout_pct",
                    "changed_at",
                    "reason"
            )
    ),

    WATERMARK_DOCUMENT(
            "watermark_document",
            "watermark_document",
            null,
            List.of(
                    "id",
                    "created_date",
                    "pdf_status",
                    "pdf_file_id",
                    "encode(sha256((token || ':salt')::bytea), 'hex') AS token"
            )
    ),

    LOTTERY_DRAW(
            "lottery_draw",
            "lottery_draw",
            null,
            List.of(
                    "id",
                    "draw_date",
                    "daily_count",
                    "bypass_count",
                    "available_slots",
                    "ticket_count",
                    "drawn_count",
                    "created_at"
            )
    ),

    LOTTERY_TICKET(
            "lottery_ticket",
            "lottery_ticket",
            null,
            List.of(
                    "id",
                    "tenant_id",
                    "status",
                    "created_at",
                    "lottery_draw_id",
                    "drawn_at",
                    "cooldown_until",
                    "cooldown_notified_at"
            )
    ),

    USER_OPERATOR(
            "user_operator",
            "user_account",
            "WHERE user_type = 'BO'",
            List.of(
                    "id",
                    "creation_date AS created_at",
                    "email",
                    "first_name",
                    "last_name",
                    "last_login_date",
                    "update_date_time AS updated_at"
            )
    );

    private final String destTableName;
    private final String sourceTableName;
    private final String whereClause;
    private final List<String> columns;

    AnalyticsTableMapping(String destTableName, String sourceTableName, String whereClause, List<String> columns) {
        this.destTableName = destTableName;
        this.sourceTableName = sourceTableName;
        this.whereClause = whereClause;
        this.columns = columns;
    }

    public String buildSelectQuery(String salt) {
        String escapedSalt = salt != null ? salt.replace("'", "''") : "";
        String selectColumns = String.join(", ", columns).replace(":salt", escapedSalt);
        StringBuilder sb = new StringBuilder("SELECT ").append(selectColumns).append(" FROM ").append(sourceTableName);
        if (whereClause != null && !whereClause.isBlank()) {
            sb.append(" ").append(whereClause);
        }
        return sb.toString();
    }
}
