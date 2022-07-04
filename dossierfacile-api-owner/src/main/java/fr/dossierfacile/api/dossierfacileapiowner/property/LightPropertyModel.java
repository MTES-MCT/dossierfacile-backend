package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.PropertyFurniture;
import fr.dossierfacile.common.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightPropertyModel {
    private Long id;
    private String ownerName;
    private String name;
    private Double rentCost;
    private Double chargesCost;
    private Double livingSpace;
    private PropertyType type;
    private PropertyFurniture furniture;
    private String address;
}
