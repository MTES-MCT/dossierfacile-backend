package fr.dossierfacile.process.file.service.document_rules.validator.french_identity_card.document_ia_model;

import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.process.file.service.document_rules.validator.document_ia.mapper.PropertyTransformer;
import fr.dossierfacile.process.file.util.IDocumentIdentity;
import lombok.Setter;

import java.util.List;

@Setter
public class DocumentIdentity implements IDocumentIdentity {

    public static class NameNormalizerTransformer implements PropertyTransformer<String, List<String>> {
        @Override
        public List<String> transform(String input) {
            if (input == null) {
                return List.of();
            }
            return List.of(input.split("/"));
        }
    }

    @DocumentIAField(twoDDocName = "nom_patronymique", extractionName = "nom")
    public String lastName;

    @DocumentIAField(twoDDocName = "nom_usage")
    public String preferredName;

    @DocumentIAField(
            twoDDocName = "liste_prenoms",
            type = DocumentIAPropertyType.STRING,
            transformer = NameNormalizerTransformer.class
    )
    public List<String> firstNames;

    @DocumentIAField(twoDDocName = "prenom", extractionName = "prenom")
    public String firstName;

    public DocumentIdentity() {
        this.lastName = null;
        this.preferredName = null;
        this.firstNames = List.of();
    }

    public DocumentIdentity(List<String> firstNames, String lastName, String preferredName) {
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.preferredName = preferredName;
    }

    public DocumentIdentity(List<String> firstNames, String lastName) {
        this.firstNames = firstNames;
        this.lastName = lastName;
    }

    @Override
    public List<String> getFirstNames() {
        if (firstNames == null || firstNames.isEmpty()) {
            if (firstName == null) {
                return List.of();
            }
            return List.of(firstName);
        }
        return firstNames;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPreferredName() {
        return preferredName;
    }

    public boolean isValid() {

        if (lastName == null && preferredName == null) {
            return false;
        }

        return !getFirstNames().isEmpty();
    }
}
