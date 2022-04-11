package fr.dossierfacile.garbagecollector.model.apartment;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name = "apartment_sharing")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentSharing implements Serializable {

    private static final long serialVersionUID = -3603815439883206021L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_dossier_pdf_document")
    private String url_dossier_pdf_document;
}
