package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.LogType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_log")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLog implements Serializable {

    private static final long serialVersionUID = 605418597255420002L;

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
    private ObjectNode jsonProfile;

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
}
