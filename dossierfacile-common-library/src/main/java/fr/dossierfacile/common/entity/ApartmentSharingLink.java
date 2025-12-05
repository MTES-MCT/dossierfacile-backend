package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.ApartmentSharingLinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "apartment_sharing_link")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE apartment_sharing_link SET deleted = true WHERE id=?")
public class ApartmentSharingLink implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    @Column
    private UUID token;

    @Column
    @Builder.Default
    private boolean fullData = false;

    @Column
    @Builder.Default
    private boolean disabled = false;

    @Column
    @Enumerated(EnumType.STRING)
    private ApartmentSharingLinkType linkType;

    @Column
    private LocalDateTime lastSentDatetime;

    @Column
    private String email;

    @Column
    @Builder.Default
    private boolean deleted = false;

    @Column
    private String title;

    @Column
    private Long createdBy;

    @Column
    private LocalDateTime expirationDate;

    @Column
    private Long partnerId;

    @Column(name = "property_id", insertable = false, updatable = false)
    private Long propertyId;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    @Builder.Default
    @Column(name = "failed_attempt_count")
    private Integer failedAttemptCount = 0;

    @Column(name = "first_failed_attempt_at")
    private LocalDateTime firstFailedAttemptAt;

}
