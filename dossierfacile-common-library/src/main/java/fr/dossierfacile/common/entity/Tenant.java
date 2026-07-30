package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService.DocumentView;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService.GuarantorView;
import fr.dossierfacile.common.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tenant")
@DiscriminatorValue("TENANT")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "lombokBuilder")
@Slf4j
public class Tenant extends User implements Person, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("id")
    private List<Guarantor> guarantors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    private String tenantFirstName;
    private String tenantLastName;
    private String tenantPreferredName;
    private String beneficiaryEmail;

    @Column
    private Integer satisfactionSurvey;

    @Column
    @Enumerated(EnumType.STRING)
    private TenantType tenantType;

    @Builder.Default
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<TenantUserApi> tenantsUserApi = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Document> documents = new ArrayList<>();

    private String zipCode;

    private Boolean abroad;

    @Builder.Default
    private Boolean honorDeclaration = Boolean.FALSE;

    @Builder.Default
    private LocalDateTime lastUpdateDate = LocalDateTime.now();

    @Column(length = 2000)
    @Size(max = 2000)
    private String clarification;

    @Column
    @Enumerated(EnumType.STRING)
    private TenantFileStatus status;

    @Column(name = "operator_date_time")
    private LocalDateTime operatorDateTime;

    private int warnings;

    private String operatorComment;

    @Column
    @Enumerated(EnumType.STRING)
    private TenantOwnerType ownerType;

    @Column(name="search_text")
    private String searchText;

    private transient String warningMessage;

    public static TenantBuilder<?, ?> builder() {
        TenantBuilder<?, ?> tenantBuilder = Tenant.lombokBuilder();
        tenantBuilder.userType(UserType.TENANT);
        return tenantBuilder;
    }

    public TenantFileStatus getStatus() {
        if (status == null) { // For tenants created before the inclusion of field "status"
            status = computeStatus();
        }
        return status;
    }


    @PrePersist
    @PreUpdate
    public void updateSearchName() {
        List<String> parts = new ArrayList<>();

        // On vérifie si c'est un THIRD_PARTY (en gérant le cas où ownerType est null)
        boolean isThirdParty = this.ownerType == TenantOwnerType.THIRD_PARTY;

        if (isThirdParty) {
            // Logique THIRD_PARTY : On prend tenantFirstName et tenantLastName
            if (this.tenantFirstName != null && !this.tenantFirstName.isBlank()) {
                parts.add(this.tenantFirstName.toLowerCase().trim());
            }
            if (this.tenantLastName != null && !this.tenantLastName.isBlank()) {
                parts.add(this.tenantLastName.toLowerCase().trim());
            }
        } else {
            // Logique par défaut (SELF ou NULL) : On prend firstName et lastName
            if (super.getFirstName() != null && !super.getFirstName().isBlank()) {
                parts.add(super.getFirstName().toLowerCase().trim());
            }
            if (super.getLastName() != null && !super.getLastName().isBlank()) {
                parts.add(super.getLastName().toLowerCase().trim());
            }
        }

        // On assemble avec un seul espace
        this.searchText = String.join(" ", parts);
    }

    public TenantFileStatus computeStatus() {
        log.info("Computing status for tenant with ID [" + getId() + "]...");
        return UpdateTenantStatusDomainService.computeStatus(
                status,
                Boolean.TRUE.equals(honorDeclaration),
                toDocumentViews(documents),
                toGuarantorViews());
    }

    public boolean isValidated() {
        return getStatus() == TenantFileStatus.VALIDATED;
    }

    public boolean isAllCategories() {
        return UpdateTenantStatusDomainService.isComplete(toDocumentViews(documents), toGuarantorViews());
    }

    private List<GuarantorView> toGuarantorViews() {
        if (guarantors == null) {
            return List.of();
        }
        return guarantors.stream()
                .map(guarantor -> new GuarantorView(guarantor.getTypeGuarantor(), toDocumentViews(guarantor.getDocuments())))
                .toList();
    }

    private static List<DocumentView> toDocumentViews(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .map(document -> new DocumentView(document.getDocumentStatus(), document.getDocumentCategory()))
                .toList();
    }

    public void lastUpdateDateProfile(LocalDateTime localDateTime, DocumentCategory documentCategory) {
        this.lastUpdateDate = localDateTime == null ? LocalDateTime.now() : localDateTime;
        if (documentCategory != null) {
            log.info("Updating document {} of the tenant", documentCategory);
        } else {
            log.info("Updating tenant profile on {}", this.lastUpdateDate);
        }
    }

    public int getTotalSalary() {
        return documents.stream().filter(d -> d.getDocumentCategory() == DocumentCategory.FINANCIAL).map(Document::getMonthlySum)
                .filter(Objects::nonNull).reduce(0, Integer::sum);
    }

    public int getGuarantorsTotalSalary() {
        return guarantors.stream().map(Guarantor::getTotalSalary).reduce(0, Integer::sum);
    }

    public String getUserFirstName() {
        return super.getFirstName();
    }

    public String getUserLastName() {
        return super.getLastName();
    }

    public String getUserPreferredName() {
        return super.getPreferredName();
    }

    public void setUserFirstName(String firstName) {
        super.setFirstName(firstName);
    }

    public void setUserLastName(String lastName) {
        super.setLastName(lastName);
    }

    public void setUserPreferredName(String preferredName) {
        super.setPreferredName(preferredName);
    }

    @Override
    public String getFirstName() {
        if (ownerType == TenantOwnerType.SELF) {
            return super.getFirstName();
        } else {
            return tenantFirstName != null ? tenantFirstName : super.getFirstName();
        }
    }

    @Override
    public String getLastName() {
        if (ownerType == TenantOwnerType.SELF) {
            return super.getLastName();
        } else {
            return tenantLastName != null ? tenantLastName : super.getLastName();
        }
    }

    @Override
    public String getPreferredName() {
        if (ownerType == TenantOwnerType.SELF) {
            return super.getPreferredName();
        }
        if (ownerType == TenantOwnerType.THIRD_PARTY) {
            return tenantPreferredName;
        }
        return super.getPreferredName();
    }

    @Override
    public void setFirstName(String firstName) {
        if (ownerType == null) {
            super.setFirstName(firstName);
            return;
        }
        if (ownerType == TenantOwnerType.SELF) {
            if (!getFranceConnect()) {
                super.setFirstName(firstName);
            }
        } else {
            this.tenantFirstName = StringUtils.trimToNull(firstName);
        }
    }

    @Override
    public void setLastName(String lastName) {
        if (ownerType == null) {
            super.setLastName(lastName);
            return;
        }
        if (ownerType == TenantOwnerType.SELF) {
            if (!getFranceConnect()) {
                super.setLastName(lastName);
            }
        } else {
            this.tenantLastName = StringUtils.trimToNull(lastName);
        }
    }

    @Override
    public void setPreferredName(String preferredName) {
        if (ownerType == null) {
            super.setPreferredName(preferredName);
            return;
        }
        if (ownerType == TenantOwnerType.SELF) {
            if (!getFranceConnect()) {
                super.setPreferredName(preferredName);
            }
        } else {
            this.tenantPreferredName = StringUtils.trimToNull(preferredName);
        }
    }

    public void setBeneficiaryEmail(String beneficiaryEmail) {
        this.beneficiaryEmail = StringUtils.trimToNull(beneficiaryEmail);
    }

    public String getNormalizedName() {
        // Only get the first name when a user has multiple first names
        var normalizedFirstName = StringUtils.stripAccents(StringUtils.trimToEmpty(getFirstName())).split(" ")[0];
        var normalizedLastName = StringUtils.stripAccents(StringUtils.trimToEmpty(getLastName()));
        if (StringUtils.isNotBlank(getPreferredName())) {
            normalizedLastName = StringUtils.stripAccents(StringUtils.trimToEmpty(getPreferredName()));
        }
        return String.format("%s_%s",
            StringUtils.capitalize(normalizedFirstName),
            StringUtils.capitalize(normalizedLastName));
    }

}
