package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template1 implements TemplateStrategy {
    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(doc.select("td[style*=font-size:24px]").text());
        prospect.setPhone(!doc.select("a[href*=tel] span").isEmpty() ? doc.select("a[href*=tel] span").get(0).text() : "");
        prospect.setEmail(fake + doc.select("a[href*=mailto] u").text());
        prospect.setPropertyId(!doc.select("td[style*=font-size:22px]").isEmpty() ? doc.select("td[style*=font-size:22px]").get(0).text().replaceAll("\\D+", "") : "");
        return prospect;
    }
}
