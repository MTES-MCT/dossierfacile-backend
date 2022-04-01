package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

public class Template5 implements TemplateStrategy {
    private static final String SELECTOR = "td[width*=295] div";

    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        prospect.setFirstName(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 1 ? doc.select(SELECTOR).get(1).text().replace("Nom : ", "") : "");
        prospect.setPhone(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 3 ? doc.select(SELECTOR).get(3).text().replace("Téléphone : ", "") : "");
        prospect.setEmail(!doc.select(SELECTOR).isEmpty() && doc.select(SELECTOR).size() > 2 ? fake + doc.select(SELECTOR).get(2).text().replace("Email : ", "") : "");
        prospect.setPropertyId(!doc.select("td[align=left] div[style*=font-family:Arial,Helvetica,sans-serif;font-size:13px;]").isEmpty() ? doc.select("td[align=left] div[style*=font-family:Arial,Helvetica,sans-serif;font-size:13px;]").get(0).text().replaceAll("\\D+", "") : "");
        if (prospect.getPropertyId().isEmpty()) {
            prospect.setPropertyId(doc.select("p span span[style*=font-size:10.0pt;font-family:\"Arial\",sans-serif;color:#555555]").text().replaceAll("\\D+", ""));
        }
        if (prospect.getPropertyId().isEmpty()) {
            prospect.setPropertyId(!doc.select("td[align=left] div[style*=font-family:Arial,Helvetica,sans-serif; font-size:13px; color:#555555]").isEmpty() ? doc.select("td[align=left] div[style*=font-family:Arial,Helvetica,sans-serif; font-size:13px; color:#555555]").get(0).text().replaceAll("\\D+", "") : "");
        }
        return prospect;
    }
}
