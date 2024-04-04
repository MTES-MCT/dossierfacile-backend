package fr.dossierfacile.api.dossierfacileapiowner.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.api.dossierfacileapiowner.property.LightPropertyModel;
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
public class OwnerModel {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean franceConnect;
    private List<LightPropertyModel> properties;
}
