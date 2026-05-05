package fr.dossierfacile.common.model.ademe;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdemeApiResultModel {
    private AdemeApiDpeJson dpe;
    private ZonedDateTime timestamp;
    private boolean success;
    private String correlationId;
    private Double consommationEnergieFinale;
}
