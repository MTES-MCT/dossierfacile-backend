package fr.gouv.owner.gmail_owner.strategies;

import fr.dossierfacile.common.entity.Prospect;
import fr.gouv.owner.gmail_owner.interfaces.TemplateStrategy;
import org.jsoup.nodes.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template6 implements TemplateStrategy {
    @Override
    public Prospect fillData(Prospect prospect, Document doc, String text, String fake) {
        Matcher m = Pattern.compile("Nom : (.+)").matcher(text);
        if (m.find()) {
            prospect.setFirstName(m.group(1));
        }
        m = Pattern.compile("Email : (.+)").matcher(text);
        if (m.find()) {
            prospect.setEmail(fake + m.group(1));
        }
        m = Pattern.compile("Tel : (.+)").matcher(text);
        if (m.find()) {
            prospect.setPhone(m.group(1));
        }
        m = Pattern.compile("référence : \"(.+)\"").matcher(text);
        if (m.find()) {
            prospect.setPropertyId(m.group(1));
        }
        return prospect;
    }
}
