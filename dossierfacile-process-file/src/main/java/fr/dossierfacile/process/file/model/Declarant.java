package fr.dossierfacile.process.file.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated
public class Declarant {
    private String nom;
    private String nomNaissance;
    private String prenoms;
    private String dateNaissance;

    @Override
    public String toString() {
        if (nomNaissance != null) {
            return "name: " + nom + " " + prenoms + ", nameOfBirth: " + nomNaissance;

        }
        if (nom != null) {
            return "name: " + nom + " " + prenoms;
        }
        return "";
    }
}

