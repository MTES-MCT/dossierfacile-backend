package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.enums.PropertyFurniture;
import fr.dossierfacile.common.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyForm {

    private Long id;

    @NotBlank
    private String name;

    private Double rentCost;

    private Double chargesCost;

    private Double livingSpace;

    private Boolean validated;

    private PropertyType type;

    private PropertyFurniture furniture;

    private String address;

    private Integer co2Emission;

    private Integer energyConsumption;

    private String dpeDate;

    private String ademeNumber;

    private Boolean dpeNotRequired;

}
