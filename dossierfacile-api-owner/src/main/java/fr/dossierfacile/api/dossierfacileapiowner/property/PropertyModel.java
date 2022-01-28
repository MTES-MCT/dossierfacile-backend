package fr.dossierfacile.api.dossierfacileapiowner.property;

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
public class PropertyModel {
    private Long id;
    private String name;
    private Double rentCost;
    private String token;
}
