package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.OwnerLogType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "owner_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OwnerLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "creation_date")
    private LocalDateTime creationDateTime;

    @Column
    @Enumerated(EnumType.STRING)
    private OwnerLogType logType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ObjectNode jsonProfile;
}
