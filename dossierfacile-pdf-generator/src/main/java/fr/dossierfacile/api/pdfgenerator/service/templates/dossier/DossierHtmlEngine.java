package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;

/**
 * Prepares the HTML engine for a template of {@code templates/dossier-pdf/}, with the fonts and the stylesheet. The
 * draft and the final PDF share this setup, so the engine lays out both of them in the same way.
 */
class DossierHtmlEngine {

    private static final String FONT_FAMILY = "Marianne";
    private static final String EMOJI_FONT_FAMILY = "Noto Emoji";

    static {
        XRLog.setLoggerImpl(new HtmlEngineLogger());
    }

    private final TemplateEngine templateEngine = createTemplateEngine();

    PdfRendererBuilder prepare(String template, Map<String, Object> variables, DossierPdfResources resources, OutputStream output) {
        String html = DossierPdfFormat.validXml(templateEngine.process(template, new Context(Locale.FRENCH, variables)));
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.usePdfUaAccessibility(true);
        builder.withProducer("DossierFacile");
        builder.useProtocolsStreamImplementation(resources, DossierPdfResources.STATIC_PROTOCOL, DossierPdfResources.MEMORY_PROTOCOL);
        useFont(builder, "Marianne-Regular.ttf", FONT_FAMILY, 400);
        useFont(builder, "Marianne-Bold.ttf", FONT_FAMILY, 700);
        useFont(builder, "Noto/NotoEmoji-Medium.ttf", EMOJI_FONT_FAMILY, 400);
        builder.withHtmlContent(html, DossierPdfResources.STATIC_PROTOCOL + ":");
        builder.toStream(output);
        return builder;
    }

    private static void useFont(PdfRendererBuilder builder, String file, String family, int weight) {
        builder.useFont(() -> {
            InputStream font = DossierHtmlEngine.class.getClassLoader().getResourceAsStream("static/fonts/" + file);
            if (font == null) {
                throw new UncheckedIOException(new IOException("Font not found: " + file));
            }
            return font;
        }, family, weight, FontStyle.NORMAL, true);
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/dossier-pdf/");
        resolver.setSuffix(".html");
        // The HTML engine reads XHTML, and the XML mode guarantees a well-formed output.
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
