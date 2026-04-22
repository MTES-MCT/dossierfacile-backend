package fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Setter;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Setter
public class BeneficiaireModel {

    @DocumentIAField(extractionName = "ligne1")
    public String ligne1;

    @DocumentIAField(extractionName = "nom")
    public String nom;

    @DocumentIAField(extractionName = "prenom")
    public String prenom;

    // ligne1 en priorité, puis concaténation des valeurs non-nulles parmi prenom / nom
    public String resolveIdentityString() {
        if (ligne1 != null) return ligne1;
        String result = Stream.of(nom, prenom)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return result.isEmpty() ? null : result;
    }
}
