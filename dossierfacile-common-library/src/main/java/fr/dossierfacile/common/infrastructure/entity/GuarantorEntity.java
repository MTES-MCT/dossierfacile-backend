package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "GuarantorEntity")
@Table(name = "guarantor")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuarantorEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String preferredName;

    @Enumerated(EnumType.STRING)
    @Column
    private TypeGuarantor typeGuarantor;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column
    private String legalPersonName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "document",
            joinColumns = @JoinColumn(name = "guarantor_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> documentIds = new ArrayList<>();
}
