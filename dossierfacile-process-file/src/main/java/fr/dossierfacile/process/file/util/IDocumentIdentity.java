package fr.dossierfacile.process.file.util;

import java.util.List;

public interface IDocumentIdentity {
    List<String> getFirstNames();

    String getLastName();

    String getPreferredName();
}
