package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template12 implements TemplateStrategy {
    private static final String SELECTOR = "td .padding span";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR).isEmpty() ? doc.select(SELECTOR).get(0).text() : "");
        prospect.setPhone(doc.select("a[href*=tel:]").attr("href").replace("tel:", ""));
        prospect.setEmail(doc.select(SELECTOR).size() > 1 ? fake + doc.select(SELECTOR).get(1).text() : "");
        prospect.setPropertyId(doc.select("td .txt14").text().split(":")[doc.select("td .txt14").text().split(":").length - 1]);
        return prospect;
    }
}
