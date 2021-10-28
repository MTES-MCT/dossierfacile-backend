package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template10 implements TemplateStrategy {
    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(doc.select("td[style*=font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#3e4649;font-weight:bold;line-height:22px] span[style*=color:#3e4649]").text());
        prospect.setPhone(doc.select("a[href*=tel]").text());
        prospect.setEmail(fake + doc.select("td a[href*=mailto] span").text());
        String[] a = doc.select("td[style*=font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#95999e;padding-left:13px;line-height:18px]").text().split("\\s");
        String propertyId = a[a.length - 1];
        prospect.setPropertyId(propertyId);
        return prospect;
    }
}
