package fr.gouv.bo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailDTO {
    private String email;
}
