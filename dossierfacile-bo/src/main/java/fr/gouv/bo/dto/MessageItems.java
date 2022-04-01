package fr.gouv.bo.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MessageItems {
    private Map<String, List<String>> checkBoxValues;
}