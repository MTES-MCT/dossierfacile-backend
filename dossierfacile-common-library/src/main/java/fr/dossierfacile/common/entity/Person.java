package fr.dossierfacile.common.entity;

import java.util.List;

public interface Person {
    String getFirstName();

    String getLastName();

    List<Document> getDocuments();
}
