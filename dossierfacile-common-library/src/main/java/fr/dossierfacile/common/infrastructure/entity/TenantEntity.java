package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.enums.AuthProvider;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantOwnerType;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.UserType;
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
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "TenantEntity")
@Table(name = "user_account")
@SecondaryTable(name = "tenant", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
@SQLRestriction("user_type = 'TENANT'")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Colonnes issues de la table principale 'user_account' ---
    // Les noms logiques doivent correspondre exactement à ceux définis dans la classe héritée User.java.

    private String firstName;

    private String lastName;

    private String preferredName;

    private String email;

    private String keycloakId;

    private Boolean franceConnect;

    private String franceConnectSub;

    private String franceConnectBirthDate;

    private String franceConnectBirthPlace;

    private String franceConnectBirthCountry;

    // Doit correspondre à @Column(name = "user_type") de User.java
    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    // Doit correspondre à @Column(name = "creation_date") de User.java
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    // Doit correspondre à @Column(name = "last_login_date") de User.java
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    private String imageUrl;

    private String acquisitionCampaign;

    private String acquisitionSource;

    private String acquisitionMedium;

    // --- Colonnes issues de la table secondaire 'tenant' ---
    // Toutes ces colonnes doivent spécifier table = "tenant".

    @Column(table = "tenant")
    private String tenantFirstName;

    @Column(table = "tenant")
    private String tenantLastName;

    @Column(table = "tenant")
    private String tenantPreferredName;

    @Column(table = "tenant")
    private Integer satisfactionSurvey;

    @Column(table = "tenant")
    @Enumerated(EnumType.STRING)
    private TenantType tenantType;

    @Column(table = "tenant")
    private String zipCode;

    @Column(table = "tenant")
    private Boolean abroad;

    @Column(table = "tenant")
    private Boolean honorDeclaration;

    @Column(table = "tenant")
    private LocalDateTime lastUpdateDate;

    @Column(table = "tenant", length = 2000)
    private String clarification;

    @Column(table = "tenant")
    @Enumerated(EnumType.STRING)
    private TenantFileStatus status;

    // Doit correspondre à @Column(name = "operator_date_time") de Tenant.java
    @Column(table = "tenant", name = "operator_date_time")
    private LocalDateTime operatorDateTime;

    @Column(table = "tenant")
    private int warnings;

    @Column(table = "tenant")
    private String operatorComment;

    @Column(table = "tenant")
    @Enumerated(EnumType.STRING)
    private TenantOwnerType ownerType;

    // Doit correspondre à @Column(name = "search_text") de Tenant.java
    @Column(table = "tenant", name = "search_text")
    private String searchText;

    // Doit correspondre à @JoinColumn(name = "apartment_sharing_id") de Tenant.java
    @Column(table = "tenant", name = "apartment_sharing_id")
    private Long apartmentSharingId;

    // --- Relations par ID uniquement (ElementCollections) ---

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "document",
            joinColumns = @JoinColumn(name = "tenant_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> documentIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "guarantor",
            joinColumns = @JoinColumn(name = "tenant_id")
    )
    @Column(name = "id")
    @Builder.Default
    private List<Long> guarantorIds = new ArrayList<>();
}
