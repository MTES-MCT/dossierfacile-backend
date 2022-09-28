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
public class Facture {
    private String numLigne;
    private String codeLigne;
    private String libelleLigne;
    private String colonne1;
    private String colonne2;
    private String colonne3;
    private String colonne4;
}
