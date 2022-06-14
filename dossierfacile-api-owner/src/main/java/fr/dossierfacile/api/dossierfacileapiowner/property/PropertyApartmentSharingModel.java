package fr.dossierfacile.api.dossierfacileapiowner.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.entity.ApartmentSharing;
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
public class PropertyApartmentSharingModel {
    private Long id;
    private ApartmentSharingModel apartmentSharing;
}
