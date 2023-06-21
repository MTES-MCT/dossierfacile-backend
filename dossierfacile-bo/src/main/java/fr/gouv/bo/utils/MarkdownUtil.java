package fr.gouv.bo.utils;

import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Map;

public class MarkdownUtil {

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .percentEncodeUrls(true)
            .nodeRendererFactory(ParagraphNodeRenderer::new)
            .attributeProviderFactory(LinkAttributeProvider::new)
            .build();

    public static String markdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }

    private static class ParagraphNodeRenderer extends CoreHtmlNodeRenderer {

        private final HtmlWriter htmlWriter;

        public ParagraphNodeRenderer(HtmlNodeRendererContext context) {
            super(context);
            this.htmlWriter = context.getWriter();
        }

        @Override
        public void visit(Paragraph paragraph) {
            // Only render children without adding <p> tags around
            visitChildren(paragraph);
            if (paragraph.getNext() != null) {
                htmlWriter.raw(" ");
            }
        }
    }

    private static class LinkAttributeProvider implements AttributeProvider {

        public LinkAttributeProvider(AttributeProviderContext context) {
        }

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof Link) {
                attributes.put("target", "_blank");
            }
        }

    }

}
