package fr.gouv.bo.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class UserModel {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}