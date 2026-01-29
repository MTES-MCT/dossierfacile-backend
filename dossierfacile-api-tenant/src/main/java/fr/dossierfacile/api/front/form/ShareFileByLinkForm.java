package fr.dossierfacile.api.front.form;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileByLinkForm {

    @NotEmpty
    private String title;

    private boolean fullData;

    private Integer daysValid;
}
