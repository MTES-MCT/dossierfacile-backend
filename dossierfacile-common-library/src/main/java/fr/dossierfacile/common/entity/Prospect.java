package fr.dossierfacile.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.dossierfacile.common.enums.TenantSituation;
import fr.dossierfacile.common.enums.TenantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
import javax.persistence.Table;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static fr.dossierfacile.common.enums.TenantType.CREATE;
import static fr.dossierfacile.common.enums.TenantType.JOIN;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "prospect")
public class Prospect implements Serializable {

    private static final long serialVersionUID = -3787344718689349120L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "creation_date")
    @Builder.Default
    @JsonIgnore
    private LocalDateTime creationDateTime = LocalDateTime.now();
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private String email;
    @Column
    private String phone;
    @Column
    private String customMessage;
    @Column
    private String propertyId;
    @ManyToOne
    @JoinColumn(name = "property_locatio_id")
    @JsonIgnore
    private Property property;
    @Column
    private TenantSituation tenantSituation;
    @Builder.Default
    @Column
    private Integer salary = 0;
    @Column
    private String guarantor;
    @Column
    @Enumerated(EnumType.STRING)
    private TenantType prospectType;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    @JsonIgnore
    private ApartmentSharing apartmentSharing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore
    private Tenant tenant;
    @Column(name = "visit_date")
    private LocalDateTime visitDate;
    @Builder.Default
    @Column(name = "visit_duration")
    private Integer visitDuration = 0;
    @Column(name = "event_id")
    private String eventId;
    @Column(name = "attendre_date")
    @JsonIgnore
    private LocalDateTime attendreDate;
    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private boolean interested = false;
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean accessFull = false;
    @Column(name = "subscription_date")
    @JsonIgnore
    private LocalDateTime subscriptionDate;
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean createAuto = false;
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean killNotification = false;
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean reminderEmailVisit = false;
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean reminderWelcome = false;

    public Prospect(String name, String lastName, String email, String phone, Integer salary, Integer situation) {
        this.firstName = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.salary = salary;
        this.creationDateTime = LocalDateTime.now();
        this.prospectType = CREATE;
        if (situation != null) {
            this.tenantSituation = TenantSituation.values()[situation];
        } else {
            this.tenantSituation = TenantSituation.CDD;
        }
    }

    public Prospect(Tenant tenant) {
        this.firstName = tenant.getFirstName();
        this.lastName = tenant.getLastName();
        this.email = tenant.getEmail().toLowerCase();
        this.phone = "";
        this.creationDateTime = LocalDateTime.now();
        this.prospectType = CREATE;
        this.tenantSituation = tenant.getTenantSituation() == TenantSituation.UNDEFINED ? TenantSituation.CDD : tenant.getTenantSituation();
        this.salary = tenant.getTotalSalary();
    }

    public Prospect(String firstName, String lastName, String email, Prospect prospect) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.creationDateTime = LocalDateTime.now();
        this.prospectType = JOIN;
        this.tenantSituation = TenantSituation.CDD;
        this.guarantor = "no";
        this.salary = 0;
        this.setPropertyId(prospect.getPropertyId());
        this.setProperty(prospect.getProperty());
        this.setApartmentSharing(prospect.getApartmentSharing());
    }

    public String getFullName() {
        return ((this.firstName != null) ? this.firstName : "") + " " + ((this.lastName != null) ? this.lastName : "");
    }

    public double getPercent() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (this.tenant.isValidated()) {
            return 100;
        }
        return 0;
    }
}
