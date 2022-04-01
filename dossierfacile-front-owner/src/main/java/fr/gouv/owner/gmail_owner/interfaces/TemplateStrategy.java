package fr.gouv.owner.gmail_owner.interfaces;

import fr.dossierfacile.common.entity.Prospect;
import org.jsoup.nodes.Document;

public interface TemplateStrategy {
    Prospect fillData(Prospect prospect, Document doc, String text, String fakeText);
}
