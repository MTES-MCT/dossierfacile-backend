package fr.dossierfacile.common.entity;

import java.util.List;

public interface Person {
    String getFirstName();

    String getLastName();

    default String getPreferredName(){
        return getLastName();
    }
    List<Document> getDocuments();
}
