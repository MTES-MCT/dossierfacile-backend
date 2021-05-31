package fr.dossierfacile.common.entity;


import fr.dossierfacile.common.enums.LogType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_log")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    public Log(LogType logType, Long tenantId, Long operatorId){
        this.logType = logType;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
    }

    public Log(LogType logType, Long tenantId){
        this.logType = logType;
        this.tenantId = tenantId;
    }
}
