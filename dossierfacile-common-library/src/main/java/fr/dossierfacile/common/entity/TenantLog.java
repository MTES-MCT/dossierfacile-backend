package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.LogType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "tenant_log")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLog implements Serializable {

    private static final long serialVersionUID = 605418597255420002L;

    private static final String DOCUMENT_SUB_CATEGORY = "documentSubCategory";
    private static final String OLD_SUM = "oldSum";
    private static final String NEW_SUM = "newSum";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long tenantId;

    @Column
    private Long operatorId;

    @Column(name = "creation_date")
    private LocalDateTime creationDateTime = LocalDateTime.now();

    @Column
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @Column(columnDefinition = "bigint[]")
    private long[] userApis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ObjectNode logDetails;

    @Column
    private Long messageId;

    public TenantLog(LogType logType, Long tenantId, Long operatorId) {
        this.logType = logType;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
    }

    public TenantLog(LogType logType, Long tenantId) {
        this.logType = logType;
        this.tenantId = tenantId;
    }

    public TenantLog(LogType logType, Long tenantId, Long operatorId, Long messageId) {
        this.logType = logType;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
        this.messageId = messageId;
    }

    public String getTitle() {
        StringBuilder builder = new StringBuilder();
        if (this.getOperatorId() != null) {
            if (this.getLogDetails() != null && this.getLogDetails().get(NEW_SUM) != null) {
                builder.append("OPERATOR_EDITED_FINANCIAL");
            } else {
                builder.append(this.getLogType().equals(LogType.ACCOUNT_EDITED) ? "OPERATOR_EDITED" : this.getLogType());
            }
            builder.append(" by opId: ");
            builder.append(this.getOperatorId());
        } else {
            builder.append(this.getLogType());
        }
        if (this.getLogDetails() != null) {
            if (this.getLogDetails().get("editionType") != null) {
                String subject = this.getLogDetails().get("fileId") != null ? " : FILE " : " : DOCUMENT ";
                builder.append(subject);
                String editionType = this.getLogDetails().get("editionType").asText().equals("ADD") ? "ADDED" : "DELETED";
                builder.append(editionType);
            }
        }
        return builder.toString();
    }

    public String getSubtitle() {
        StringBuilder builder = new StringBuilder();
        ObjectNode details = this.getLogDetails();
        if (details != null && details.get("documentCategory") != null && details.get(DOCUMENT_SUB_CATEGORY) != null) {
            if (details.get("guarantorId") != null) {
                builder.append("GUARANTOR - ");
            }
            builder.append(details.get("documentCategory").asText());
            builder.append(" - ");
            builder.append(details.get(DOCUMENT_SUB_CATEGORY).asText());
            if (details.get("fileName") != null) {
                builder.append(" - ");
                builder.append(details.get("fileName").asText());
            }
        }
        if (details != null && details.get(OLD_SUM) != null && details.get(NEW_SUM) != null) {
            builder.append(" - OLD AMOUNT: ");
            builder.append(details.get(OLD_SUM).asInt());
            builder.append(" - NEW AMOUNT: ");
            builder.append(details.get(NEW_SUM).asInt());
        }
        return builder.toString();
    }

    public String getModificationText() {
        StringBuilder builder = new StringBuilder();
        ObjectNode details = this.getLogDetails();
        if (details != null
          && details.get(DOCUMENT_SUB_CATEGORY) != null
          && details.get(OLD_SUM) != null
          && details.get(NEW_SUM) != null) {
            builder.append(details.get(DOCUMENT_SUB_CATEGORY).asText());
            builder.append(" - Modification du ");
            builder.append(this.getCreationDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yy")));
            builder.append(" - Ancien montant ");
            builder.append(details.get(OLD_SUM).asInt());
            builder.append(" - Nouveau montant ");
            builder.append(details.get(NEW_SUM).asInt());
        }
        return builder.toString();
    }
}
