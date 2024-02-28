package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "guarantor")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Guarantor implements Person, Serializable {

    private static final long serialVersionUID = -3601815435883206221L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    @Enumerated(EnumType.STRING)
    private TypeGuarantor typeGuarantor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Builder.Default
    @OneToMany(mappedBy = "guarantor", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Document> documents = new ArrayList<>();

    private String legalPersonName;

    public String getCompleteName() {
        StringBuilder fullName = new StringBuilder();
        if (typeGuarantor == TypeGuarantor.NATURAL_PERSON) {
            if (!firstName.isBlank() && firstName != null){
                fullName.append(firstName);
            }
            if (!lastName.isBlank() && lastName != null){
                fullName.append(" ").append(lastName);
            }
        } else if (typeGuarantor == TypeGuarantor.LEGAL_PERSON) {
            if (!legalPersonName.isBlank() && legalPersonName != null){
                fullName.append(legalPersonName);
            }
        }
        return fullName.toString();
    }

    public int getTotalSalary() {
        return documents.stream().filter(d -> d.getDocumentCategory() == DocumentCategory.FINANCIAL).map(Document::getMonthlySum)
                .filter(Objects::nonNull).reduce(0, Integer::sum);
    }

}
