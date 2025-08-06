package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileStorageStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Document implements Serializable {

    private static final long serialVersionUID = -3603815939453106021L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "name")
    private String name = java.util.UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    private DocumentSubCategory documentSubCategory;

    @Enumerated(EnumType.STRING)
    private DocumentCategoryStep documentCategoryStep;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @ToString.Exclude
    private Tenant tenant;

    @Builder.Default
    @Column(name = "creation_date")
    private LocalDateTime creationDateTime = LocalDateTime.now();

    @Builder.Default
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id")
    @ToString.Exclude
    private Guarantor guarantor;

    private String customText;

    private Boolean noDocument;

    private Integer monthlySum;

    @Builder.Default
    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private List<File> files = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.TO_PROCESS;

    @OneToOne
    @JoinColumn(name = "watermark_file_id")
    private StorageFile watermarkFile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_denied_reasons_id")
    @ToString.Exclude
    private DocumentDeniedReasons documentDeniedReasons;

    @Builder.Default
    private Boolean avisDetected = false;

    @Nullable
    @OneToOne(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private DocumentAnalysisReport documentAnalysisReport;

    @PreRemove
    void deleteCascade() {
        if (watermarkFile != null)
            watermarkFile.setStatus(FileStorageStatus.TO_DELETE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Document document = (Document) o;
        return id != null && Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getDocumentName() {
        return String.format(getDocumentNameFormat(), id);
    }

    private String getDocumentNameFormat() {
        return switch (getDocumentSubCategory()) {
            case FRENCH_IDENTITY_CARD, FRENCH_PASSPORT, FRENCH_RESIDENCE_PERMIT, DRIVERS_LICENSE, FRANCE_IDENTITE,
                 OTHER_IDENTIFICATION -> "Identité_%s.pdf";
            case TENANT, OWNER, GUEST_PARENTS, GUEST, GUEST_COMPANY, GUEST_ORGANISM, SHORT_TERM_RENTAL,
                 OTHER_RESIDENCY -> "Hébergement_%s.pdf";
            case CDI -> "Activité_cdi_%s.pdf";
            case CDD -> "Activité_cdd_%s.pdf";
            case ALTERNATION -> "Activité_alternance_%s.pdf";
            case INTERNSHIP -> "Activité_stage_%s.pdf";
            case STUDENT -> "Activité_études_%s.pdf";
            case PUBLIC -> "Activité_public_%s.pdf";
            case CTT -> "Activité_ctt_%s.pdf";
            case RETIRED -> "Activité_retraite_%s.pdf";
            case UNEMPLOYED -> "Activité_chomage_%s.pdf";
            case INDEPENDENT -> "Activité_indépendant_%s.pdf";
            case INTERMITTENT -> "Activité_intermittent_%s.pdf";
            case STAY_AT_HOME_PARENT -> "Activité_au_foyer_%s.pdf";
            case NO_ACTIVITY -> "Activité_sans_emploi_%s.pdf";
            case ARTIST -> "Activité_artiste_auteur_%s.pdf";
            case OTHER -> "Activité_autre_%s.pdf";
            case SALARY -> "Ressources_revenus_%s.pdf";
            case SCHOLARSHIP -> "Ressources_bourse_%s.pdf";
            case SOCIAL_SERVICE -> "Ressources_aides_sociales_%s.pdf";
            case RENT -> "Ressources_rentes_%s.pdf";
            case PENSION -> "Ressources_pensions_%s.pdf";
            case NO_INCOME -> "Ressources_sans_revenus_%s.pdf";
            case MY_NAME, MY_PARENTS, LESS_THAN_YEAR, OTHER_TAX -> "Imposition_%s.pdf";
            case CERTIFICATE_VISA -> "garant_visa_%s.pdf";
            case VISALE -> "garant_visale_%s.pdf";
            case OTHER_GUARANTEE -> "garant_autre_%s.pdf";
            case LEGAL_PERSON -> "garant_personne_morale_%s.pdf";
            default -> getDocumentCategory().getText() + "_%s.pdf";
        };
    }
}
