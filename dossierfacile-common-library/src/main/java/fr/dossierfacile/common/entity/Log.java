package fr.dossierfacile.common.entity;


import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_log")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Log implements Serializable {

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

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Object jsonProfile;

    @Column
    private Long messageId;

    public Log(LogType logType, Long tenantId, Long operatorId) {
        this.logType = logType;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
    }

    public Log(LogType logType, Long tenantId) {
        this.logType = logType;
        this.tenantId = tenantId;
    }

    public Log(LogType logType, Long tenantId, Long operatorId, Long messageId) {
        this.logType = logType;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
        this.messageId = messageId;
    }
}
