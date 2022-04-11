package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template2 implements TemplateStrategy {
    private static final String SELECTOR = "td[width=580] span";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR).isEmpty() ? doc.select(SELECTOR).get(0).text() : "");
        prospect.setPhone(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 1 ? doc.select(SELECTOR).get(1).text() : "");
        prospect.setEmail(fake + doc.select("td[width=580] a").text());
        prospect.setPropertyId(doc.select("span[style*=font-style:italic]").text().replaceAll("\\D+", ""));
        return prospect;
    }
}
