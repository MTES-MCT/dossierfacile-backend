package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.enums.PropertyFurniture;
import fr.dossierfacile.common.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyModel {
    private Long id;
    private String name;
    private Double rentCost;
    private Double chargesCost;
    private Double livingSpace;
    private String token;
    private Boolean validated;
    private List<PropertyApartmentSharingModel> propertiesApartmentSharing;
    private PropertyType type;
    private PropertyFurniture furniture;
    private String address;
    private Integer co2Emission;
    private Integer energyConsumption;
    private String dpeDate;
    private String ademeNumber;
    private ObjectNode ademeApiResult;
}
