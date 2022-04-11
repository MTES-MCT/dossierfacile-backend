package fr.gouv.bo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultDTO {
    private Long id;
    private String result;

    public ResultDTO(Long id, String result) {
        this.id = id;
        this.result = result;
    }
}
