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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apartment_sharing_link")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
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
    private String token;

    @Column
    private boolean fullData = false;

    @Column
    private boolean disabled = false;

    @Column
    @Enumerated(EnumType.STRING)
    private ApartmentSharingLinkType linkType;

    @Column
    private LocalDateTime lastSentDatetime;

    @Column
    private String email;

}
