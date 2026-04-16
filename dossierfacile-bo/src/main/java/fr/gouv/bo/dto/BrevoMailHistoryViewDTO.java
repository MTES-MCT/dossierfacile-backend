package fr.gouv.bo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrevoMailHistoryViewDTO {
    private List<BrevoMailStatusDTO> items;
    private boolean error;
    private String errorMessage;

    public boolean isEmpty() {
        return !error && (items == null || items.isEmpty());
    }
}
