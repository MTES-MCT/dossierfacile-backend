package fr.dossierfacile.api.front.model.property;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyOModel {

    private Long id;
    private String name;
    private String address;
    private Double rentCost;
    private OwnerModel owner;
}
