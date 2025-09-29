package fr.dossierfacile.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.io.Serializable;

@Builder
@Entity
@Table(name = "property_apartment_sharing")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropertyApartmentSharing implements Serializable {

    private static final long serialVersionUID = -5341936604135388675L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    @NotFound(action = NotFoundAction.IGNORE)
    @ManyToOne
    @JoinColumn(name = "apartment_sharing_id")
    private ApartmentSharing apartmentSharing;

    @Builder.Default
    @Column(nullable = false)
    private boolean accessFull = false;
}

