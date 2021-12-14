package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template11 implements TemplateStrategy {
    private static final String SELECTOR = "td[style*=padding-top:10px] span";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR).isEmpty() ? doc.select(SELECTOR).get(0).text().replace("Prénom : ", "") : "");
        prospect.setLastName(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 1 ? doc.select(SELECTOR).get(1).text().replace("Nom : ", "") : "");
        prospect.setPhone(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 3 ? doc.select(SELECTOR).get(3).text().replace("Tél. : ", "") : "");
        prospect.setEmail(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 2 ? fake + doc.select(SELECTOR).get(2).text().replace("E-mail : ", "") : "");
        prospect.setPropertyId(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 4 ? doc.select(SELECTOR).get(4).text().replace("Référence de l'annonce : ", "") : "");
        return prospect;
    }
}
