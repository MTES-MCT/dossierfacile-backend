package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guarantor")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Guarantor implements Serializable {

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
}
