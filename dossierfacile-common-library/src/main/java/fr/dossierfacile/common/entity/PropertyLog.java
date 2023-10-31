package fr.dossierfacile.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
import java.time.LocalDateTime;

import static fr.dossierfacile.common.entity.PropertyLogType.*;

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
