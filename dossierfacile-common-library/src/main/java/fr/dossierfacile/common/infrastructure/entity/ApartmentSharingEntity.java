package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "ApartmentSharingEntity")
@Table(name = "apartment_sharing")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentSharingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_date")
    private LocalDateTime operatorDateTime;

    @Enumerated(EnumType.STRING)
    private ApplicationType applicationType;

    @Column(name = "pdf_dossier_file_id")
    private Long pdfDossierFileId;

    @Enumerated(EnumType.STRING)
    private FileStatus dossierPdfDocumentStatus;

    @LastModifiedDate
    private LocalDateTime lastUpdateDate;

    // --- Relations par ID uniquement (ElementCollections) ---

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tenant",
            joinColumns = @JoinColumn(name = "apartment_sharing_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> tenantIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_apartment_sharing",
            joinColumns = @JoinColumn(name = "apartment_sharing_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> propertyApartmentSharingIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "apartment_sharing_link",
            joinColumns = @JoinColumn(name = "apartment_sharing_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> apartmentSharingLinkIds = new ArrayList<>();
}
