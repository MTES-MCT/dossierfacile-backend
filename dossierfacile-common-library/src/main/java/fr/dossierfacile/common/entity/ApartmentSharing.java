package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "apartment_sharing")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApartmentSharing implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Tenant> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<PropertyApartmentSharing> propertiesApartmentSharing = new ArrayList<>();

    @Column(name = "operator_date")
    private LocalDateTime operatorDateTime;

    @Column
    @Enumerated(EnumType.STRING)
    private ApplicationType applicationType;

    @OneToOne
    @JoinColumn(name = "pdf_dossier_file_id")
    private StorageFile pdfDossierFile;

    @Column
    @Enumerated(EnumType.STRING)
    private FileStatus dossierPdfDocumentStatus;

    @LastModifiedDate
    private LocalDateTime lastUpdateDate;

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<ApartmentSharingLink> apartmentSharingLinks = new ArrayList<>();

    @PreRemove
    void deleteCascade() {
        if (pdfDossierFile != null)
            pdfDossierFile.setStatus(FileStorageStatus.TO_DELETE);
    }

    public ApartmentSharing(Tenant tenant) {
        tenants.add(tenant);
        this.applicationType = ApplicationType.ALONE;
    }

    public TenantFileStatus getStatus() {
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.DECLINED) {
                return TenantFileStatus.DECLINED;
            }
        }
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.INCOMPLETE) {
                return TenantFileStatus.INCOMPLETE;
            }
        }
        int archivedTenantCount = 0;
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.ARCHIVED) {
                archivedTenantCount++;
            }
        }
        if (archivedTenantCount > 0) {
            if (archivedTenantCount == tenants.size()) {
                return TenantFileStatus.ARCHIVED;
            }
            return TenantFileStatus.INCOMPLETE;
        }
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.TO_PROCESS) {
                return TenantFileStatus.TO_PROCESS;
            }
        }
        return TenantFileStatus.VALIDATED;
    }

    public int getNumberOfTenants() {
        return tenants.size();
    }

    public Integer totalSalary() {
        int totalSalary = 0;
        for (Tenant tenant : tenants) {
            totalSalary += tenant.getTotalSalary();
        }
        return totalSalary;
    }

    public Integer totalGuarantorSalary() {
        int totalSalary = 0;
        for (Tenant tenant : tenants) {
            totalSalary += tenant.getGuarantorsTotalSalary();
        }
        return totalSalary;
    }

    public Optional<Tenant> getOwnerTenant() {
        return tenants.stream().filter(it -> it.getTenantType() == TenantType.CREATE).findFirst();
    }
}
