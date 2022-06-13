package fr.dossierfacile.common.entity;


import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Tenant> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Prospect> prospects = new ArrayList<>();

    @OneToMany(mappedBy = "apartmentSharing", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<PropertyApartmentSharing> propertiesApartmentSharing = new ArrayList<>();

    @Column
    private String token;

    @Column
    private String tokenPublic;

    @Column(name = "operator_date")
    private LocalDateTime operatorDateTime;

    @Column
    @Enumerated(EnumType.STRING)
    private ApplicationType applicationType;

    private String urlDossierPdfDocument;

    @Column
    @Enumerated(EnumType.STRING)
    private FileStatus dossierPdfDocumentStatus;

    public ApartmentSharing(Tenant tenant) {
        tenants.add(tenant);
        this.token = UUID.randomUUID().toString();
        this.tokenPublic = UUID.randomUUID().toString();
    }

    public TenantFileStatus getStatus() {
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.INCOMPLETE) {
                return TenantFileStatus.INCOMPLETE;
            }
        }
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantFileStatus.DECLINED) {
                return TenantFileStatus.DECLINED;
            }
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

    public int getNumberOfCompleteRegister() {
        return (int) tenants.stream().filter(t -> t.getStatus() != TenantFileStatus.INCOMPLETE).count();
    }

    public void addProspect(Prospect prospect) {
        prospects.add(prospect);
        prospect.setApartmentSharing(this);
    }

    public int getTotalGuarantor() {
        int total = 0;
        for (Tenant tenant : tenants) {
            if (tenant.getGuarantors() != null) {
                total += tenant.getGuarantors().size();
            }
        }
        return total;
    }

    public Integer totalSalary() {
        int totalSalary = 0;
        for (Tenant tenant : tenants) {
            totalSalary += tenant.getTotalSalary();
        }
        return totalSalary;
    }

    public List<TenantUserApi> groupingAllTenantUserApisInTheApartment() {
        List<TenantUserApi> tenantUserApis = new ArrayList<>();
        if (tenants != null && !tenants.isEmpty()) {
            tenants.stream()
                    .filter(t -> t.getTenantsUserApi() != null && !t.getTenantsUserApi().isEmpty())
                    .forEach(t -> tenantUserApis.addAll(t.getTenantsUserApi()));
        }
        return tenantUserApis;
    }

    public Integer totalGuarantorSalary() {
        int totalSalary = 0;
        for (Tenant tenant : tenants) {
            totalSalary += tenant.getGuarantorsTotalSalary();
        }
        return totalSalary;
    }
}
