package fr.dossierfacile.document.analysis.rule.validator.tax.document_ia_model;

import fr.dossierfacile.common.utils.IDocumentIdentity;

import java.util.List;

public class TaxName implements IDocumentIdentity {

    private List<String> firstNames = List.of();
    private String lastName = "";

    public TaxName(List<String> firstNames, String lastName) {
        this.firstNames = firstNames;
        this.lastName = lastName;
    }

    @Override
    public List<String> getFirstNames() {
        return firstNames;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPreferredName() {
        return "";
    }
}
