package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template8 implements TemplateStrategy {
    private static final String SELECTOR1 = "td p[style*=line-height:13.5pt] span span[style*=font-size:9.0pt;font-family:\"Arial\",sans-serif;color:#95999e]";
    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(doc.select("td p span b span[style*=font-size:10.5pt;font-family:\"Arial\",sans-serif;color:#3e4649]").text());
        prospect.setPhone(doc.select("a[href*=tel] span b span[style*=font-size:10.5pt;font-family:\"Arial\",sans-serif;color:#6a6eaa]").text().replaceAll("\\s", ""));
        prospect.setEmail(fake + doc.select("a[href*=mailto] span span span[style*=font-size:10.5pt;font-family:\"Arial\",sans-serif;color:#656d78]").text());
        prospect.setPropertyId(!doc.select(SELECTOR1).isEmpty() && doc.select(SELECTOR1).size() > 2 ? doc.select(SELECTOR1).get(2).text().replaceAll("\\D", "") : "");
        return prospect;
    }
}
