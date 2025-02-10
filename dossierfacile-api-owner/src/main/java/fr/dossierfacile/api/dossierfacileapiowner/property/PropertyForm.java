package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.api.dossierfacileapiowner.annotation.ValidDpeDate;
import fr.dossierfacile.common.enums.PropertyFurniture;
import fr.dossierfacile.common.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyForm {

    private Long id;

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

    @ValidDpeDate
    private String dpeDate;

    private String ademeNumber;

    private Boolean dpeNotRequired;

}
