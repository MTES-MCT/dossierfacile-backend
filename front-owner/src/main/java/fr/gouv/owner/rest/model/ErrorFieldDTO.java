package fr.gouv.owner.rest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorFieldDTO {
    String field;
    String rejectedValue;
    String message;
}
