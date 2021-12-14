package fr.gouv.owner.dto;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Property;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class PropertyDTO {

    @NotBlank
    private String name;
    private Owner owner;
    private String propertyId;
    private double rentCost;

    public PropertyDTO(Property property) {
        this.name = property.getName();
        this.setOwner(property.getOwner());
        this.propertyId = property.getPropertyId();
        this.rentCost = property.getRentCost();
    }
}
