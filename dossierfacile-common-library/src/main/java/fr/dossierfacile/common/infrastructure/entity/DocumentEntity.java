package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "DocumentEntity")
@Table(name = "document")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @Builder.Default
    private String name = java.util.UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    private DocumentSubCategory documentSubCategory;

    @Enumerated(EnumType.STRING)
    private DocumentCategoryStep documentCategoryStep;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "creation_date")
    @Builder.Default
    private LocalDateTime creationDateTime = LocalDateTime.now();

    @Column(name = "last_modified_date")
    @Builder.Default
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @Column(name = "guarantor_id")
    private Long guarantorId;

    private String customText;

    private Boolean noDocument;

    private Integer monthlySum;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.TO_PROCESS;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "watermark_file_id")
    private StorageFile watermarkFile;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "document_denied_reasons_id")
    private DocumentDeniedReasons documentDeniedReasons;

    @Builder.Default
    private Boolean avisDetected = false;

    @OneToMany(mappedBy = "document", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();
}
