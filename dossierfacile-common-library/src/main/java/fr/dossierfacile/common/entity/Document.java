package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.converter.TaxDocumentConverter;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.type.TaxDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Document implements Serializable {

    private static final long serialVersionUID = -3603815939453106021L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    private DocumentSubCategory documentSubCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Builder.Default
    @Column(name = "creation_date")
    private LocalDateTime creationDateTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id")
    private Guarantor guarantor;

    private String customText;

    private Boolean noDocument;

    private Integer monthlySum;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<File> files = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.TO_PROCESS;

    private String name;

    @Column(columnDefinition = "text")
    @Convert(converter = TaxDocumentConverter.class)
    private TaxDocument taxProcessResult;


    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;


    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;

    @Builder.Default
    private int retries = 0;

    private boolean locked;

    private String lockedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_denied_reasons_id")
    private DocumentDeniedReasons documentDeniedReasons;

    @Basic(fetch = FetchType.LAZY)
    public boolean isLocked() {
        return locked;
    }

    @Basic(fetch = FetchType.LAZY)
    public String getLockedBy() {
        return lockedBy;
    }
}
