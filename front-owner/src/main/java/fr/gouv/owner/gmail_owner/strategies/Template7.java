package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template7 implements TemplateStrategy {
    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(doc.select("span strong span[style*=font-size:13.5pt;font-family:Roboto-Medium;color:#5d728b]").text());
        prospect.setPhone(!doc.select("span[style*=font-size:11.5pt;font-family:Roboto-Medium;color:#5d728b] strong span[style*=font-family:Roboto-Medium]").isEmpty() ? doc.select("span[style*=font-size:11.5pt;font-family:Roboto-Medium;color:#5d728b] strong span[style*=font-family:Roboto-Medium]").get(0).text().replaceAll("\\s", "") : "");
        prospect.setEmail(fake + doc.select("strong span[style*=font-family:Roboto-Medium] a[href*=mailto]").text());
        prospect.setPropertyId(doc.select("b span[style*=font-size:11.5pt]").text());
        return prospect;
    }
}
