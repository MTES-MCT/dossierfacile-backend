package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;


@Entity
@Table(name = "operator_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OperatorLog {

    private static final long serialVersionUID = -1455029537491748394L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(name = "tenant_status")
    @Enumerated(EnumType.STRING)
    private TenantFileStatus tenantFileStatus;

    @Column(name = "action_type")
    @Enumerated(EnumType.STRING)
    private ActionOperatorType actionOperatorType;

    private LocalDateTime creationDate;

    private Integer processedDocuments;

    @Column(name = "time_spent")
    private Integer timeSpent;

    public OperatorLog(Tenant tenant, User operator, TenantFileStatus tenantFileStatus, ActionOperatorType actionOperatorType, Integer processedDocuments, Integer timeSpent) {
        this.tenant = tenant;
        this.operator = operator;
        this.tenantFileStatus = tenantFileStatus;
        this.actionOperatorType = actionOperatorType;
        creationDate = LocalDateTime.now();
        this.processedDocuments = processedDocuments;
        this.timeSpent = timeSpent;
    }

    public OperatorLog(Tenant tenant, User operator, TenantFileStatus tenantFileStatus, ActionOperatorType actionOperatorType) {
        this(tenant, operator, tenantFileStatus, actionOperatorType, null, null);
    }

}
