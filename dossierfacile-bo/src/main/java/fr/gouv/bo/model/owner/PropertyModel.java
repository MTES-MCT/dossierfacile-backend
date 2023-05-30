package fr.gouv.bo.model.owner;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.PropertyFurniture;
import fr.dossierfacile.common.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyModel {
    private Long id;
    //@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private String creationDateTime;
    private String token;
    private String name;
    private String propertyId;
    private Integer countVisit;
    private Double rentCost;
    private Double chargesCost;
    private Double livingSpace;
    //private List<Long> propertiesApartmentSharing;
    //private List<Long> prospects;
    //private List<Long> mergedPropertyId;
    private Boolean notification;
    private Boolean displayed;
    private Integer cantEmailSentProspect;
    private Boolean validated;
    private PropertyType type;
    private PropertyFurniture furniture;
    private Integer energyConsumption;
    private Integer co2Emission;
    private String address;
}
