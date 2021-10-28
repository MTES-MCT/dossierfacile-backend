package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template3 implements TemplateStrategy {
    private static final String SELECTOR1 = "td[style*=padding-top:10px] span";
    private static final String SELECTOR2 = "td[style*=padding-top:10px] span a";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR1).isEmpty() && doc.select(SELECTOR1).size() > 1 ? doc.select(SELECTOR1).get(1).text().replace("Nom : ", "") : "");
        prospect.setPhone(!doc.select(SELECTOR2).isEmpty() && doc.select(SELECTOR1).size() > 1 ? doc.select(SELECTOR2).get(1).text() : "");
        prospect.setEmail(!doc.select(SELECTOR2).isEmpty() ? fake + doc.select(SELECTOR2).get(0).text() : "");
        prospect.setPropertyId(!doc.select(SELECTOR1).isEmpty() && doc.select(SELECTOR1).size() > 4 ? doc.select(SELECTOR1).get(4).text().replace("Référence de l'annonce : ", "") : "");
        return prospect;
    }
}
