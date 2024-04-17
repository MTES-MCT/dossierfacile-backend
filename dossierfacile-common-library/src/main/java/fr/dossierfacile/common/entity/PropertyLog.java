package fr.dossierfacile.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static fr.dossierfacile.common.entity.PropertyLogType.APPLICATION_DELETED_BY_OWNER;
import static fr.dossierfacile.common.entity.PropertyLogType.APPLICATION_PAGE_VISITED;
import static fr.dossierfacile.common.entity.PropertyLogType.APPLICATION_RECEIVED;

@Entity
@Table(name = "property_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropertyLog {

    private static final long serialVersionUID = -3603846262626208324L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    private LocalDateTime creationDate;

    @Enumerated(EnumType.STRING)
    private PropertyLogType logType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    private PropertyLog(Property property, PropertyLogType logType, ApartmentSharing apartmentSharing) {
        this.property = property;
        this.logType = logType;
        this.apartmentSharing = apartmentSharing;
        creationDate = LocalDateTime.now();
    }

    public static PropertyLog applicationPageVisited(Property property) {
        return new PropertyLog(property, APPLICATION_PAGE_VISITED, null);
    }

    public static PropertyLog applicationReceived(Property property, ApartmentSharing apartmentSharing) {
        return new PropertyLog(property, APPLICATION_RECEIVED, apartmentSharing);
    }

    public static PropertyLog applicationDeletedByOwner(PropertyApartmentSharing propertyApartmentSharing) {
        Property property = propertyApartmentSharing.getProperty();
        ApartmentSharing apartmentSharing = propertyApartmentSharing.getApartmentSharing();
        return new PropertyLog(property, APPLICATION_DELETED_BY_OWNER, apartmentSharing);
    }

}
