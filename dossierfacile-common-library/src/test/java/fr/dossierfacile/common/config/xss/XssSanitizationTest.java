package fr.dossierfacile.common.config.xss;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XssSanitizationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new XssStringJsonDeserializer());
        objectMapper.registerModule(module);
    }

    static class SampleDto {
        private String name;
        private String comment;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    @Test
    void deserialize_shouldStripXssScriptTagsFromJsonBody() throws Exception {
        String json = "{\"name\":\"John<script>alert('XSS')</script>\",\"comment\":\"<img src=x onerror=alert(1)>Hello\"}";

        SampleDto dto = objectMapper.readValue(json, SampleDto.class);

        assertThat(dto.getName()).isEqualTo("John");
        assertThat(dto.getComment()).isEqualTo("Hello");
    }

    @Test
    void deserialize_shouldKeepNormalTextIntact() throws Exception {
        String json = "{\"name\":\"Jean-Pierre D'Arc\",\"comment\":\"Valid text with & and numbers 123\"}";

        SampleDto dto = objectMapper.readValue(json, SampleDto.class);

        assertThat(dto.getName()).isEqualTo("Jean-Pierre D'Arc");
        assertThat(dto.getComment()).isEqualTo("Valid text with & and numbers 123");
    }

    @Test
    void deserialize_shouldNotRecreateHtmlFromPreEncodedEntities() throws Exception {
        String json = "{\"name\":\"&lt;script&gt;alert(1)&lt;/script&gt;\",\"comment\":\"Test &lt;img src=x onerror=alert(2)&gt;\"}";

        SampleDto dto = objectMapper.readValue(json, SampleDto.class);

        // Pre-encoded script tags are unescaped BEFORE clean so Jsoup strips the tag completely (""),
        // rather than re-creating raw <script> HTML tags in the String output.
        assertThat(dto.getName()).isEqualTo("");
        assertThat(dto.getComment()).isEqualTo("Test");
    }

    @Test
    void cleanHtml_shouldStripScriptAndDangerousTags() {
        String input = "<p>Hello <script>alert('XSS')</script><img src=x onerror=alert(1)>world</p>";
        String cleaned = XssSanitizer.cleanHtml(input);

        assertThat(cleaned).doesNotContain("<script>");
        assertThat(cleaned).doesNotContain("onerror");
        assertThat(cleaned).contains("<p>Hello world</p>");
    }

    @Test
    void cleanHtml_shouldPreserveFormattingAndLinks() {
        String input = "<p>Bonjour,</p><ul><li>Document non conforme</li></ul><p>Consulter <a href=\"/contact?open=form\">support</a></p>";
        String cleaned = XssSanitizer.cleanHtml(input);

        assertThat(cleaned).contains("<p>Bonjour,</p>");
        assertThat(cleaned).contains("<ul>");
        assertThat(cleaned).contains("<li>Document non conforme</li>");
        assertThat(cleaned).contains("<a href=\"/contact?open=form\">support</a>");
    }

    @Test
    void cleanHtml_shouldAddNofollowToExternalLinks() {
        String input = "<a href=\"https://example.com\">External</a>";
        String cleaned = XssSanitizer.cleanHtml(input);

        assertThat(cleaned).contains("<a href=\"https://example.com\" rel=\"nofollow\">External</a>");
    }

    @Test
    void cleanHtml_shouldStripJavascriptLinks() {
        String input = "<a href=\"javascript:alert(1)\">Click me</a>";
        String cleaned = XssSanitizer.cleanHtml(input);

        assertThat(cleaned).doesNotContain("javascript:");
        assertThat(cleaned).contains("<a rel=\"nofollow\">Click me</a>");
    }

    @Test
    void cleanHtml_shouldReturnNullForNullInput() {
        assertThat(XssSanitizer.cleanHtml(null)).isNull();
    }
}
