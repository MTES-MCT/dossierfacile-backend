package fr.dossierfacile.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_log")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "creation_date")
    private LocalDateTime creationDateTime = LocalDateTime.now();
    private String applicationName;
    private Integer apiVersion;
    @Column
    @Enumerated(EnumType.STRING)
    private ApplicationLogType logType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object details;

    public enum ApplicationLogType {
        UPGRADE
    }
}