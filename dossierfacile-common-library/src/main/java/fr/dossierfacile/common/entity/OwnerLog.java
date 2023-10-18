package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.OwnerLogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Object jsonProfile;
}
