package fr.dossierfacile.common.utils;

import java.util.List;

public class TestDocumentIdentity implements IDocumentIdentity {

    private final List<String> firstNames;
    private final String lastName;
    private final String preferredName;

    public TestDocumentIdentity(List<String> firstNames, String lastName, String preferredName) {
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.preferredName = preferredName;
    }

    public TestDocumentIdentity(List<String> firstNames, String lastName) {
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.preferredName = null;
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
        return preferredName;
    }
}
