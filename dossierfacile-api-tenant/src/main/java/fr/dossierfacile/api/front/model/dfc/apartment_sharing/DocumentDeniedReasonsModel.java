package fr.dossierfacile.api.front.model.dfc.apartment_sharing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDeniedReasonsModel {
    private List<String> checkedOptions;
    private List<Integer> checkedOptionsId;
    private List<SelectedOption> selectedOptions;
    private boolean messageData;
    private String comment;
}
