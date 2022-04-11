package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template13 implements TemplateStrategy {
    private static final String SELECTOR = "td[style*=padding:20px; line-height:25px] strong";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR).isEmpty() ? doc.select(SELECTOR).get(0).text() : "");
        prospect.setPhone(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 1 ? doc.select(SELECTOR).get(1).text().replaceAll("\\s", "") : "");
        prospect.setEmail(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 2 ? fake + doc.select(SELECTOR).get(2).text() : "");
        prospect.setPropertyId(doc.select("td div strong a").text());
        return prospect;
    }
}
