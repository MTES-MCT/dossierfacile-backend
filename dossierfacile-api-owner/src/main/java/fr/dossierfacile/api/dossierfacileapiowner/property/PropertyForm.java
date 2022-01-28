package fr.dossierfacile.api.dossierfacileapiowner.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyForm {

    private Long id;

    @NotBlank
    private String name;

    private Double rentCost;
}
