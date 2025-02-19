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
}
