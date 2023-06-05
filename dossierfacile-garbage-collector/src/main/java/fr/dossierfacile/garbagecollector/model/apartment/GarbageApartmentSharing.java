package fr.dossierfacile.garbagecollector.model.apartment;


import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name = "apartment_sharing")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GarbageApartmentSharing implements Serializable {

    private static final long serialVersionUID = -3603815439883206021L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "pdf_dossier_file_id")
    private StorageFile pdfDossierFile;

    @Column
    @Enumerated(EnumType.STRING)
    private FileStatus dossierPdfDocumentStatus;
}
