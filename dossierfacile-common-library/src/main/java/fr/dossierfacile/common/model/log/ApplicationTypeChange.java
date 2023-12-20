package fr.dossierfacile.common.model.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationTypeChange {

    private ApplicationType oldType;
    private ApplicationType newType;

}
