package fr.gouv.bo.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static fr.gouv.bo.utils.MarkdownUtil.markdownToHtml;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownUtilTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            A message, A message
            An *important* **message**, An <em>important</em> <strong>message</strong>
            This is a [link](url), This is a <a rel="nofollow" href="url" target="_blank">link</a>
            """)
    void should_support_markdown_syntax(String input, String expected) {
        assertThat(markdownToHtml(input)).isEqualTo(expected);
    }

    @Test
    void should_ignore_new_lines() {
        assertThat(markdownToHtml("""
                Abc
                                
                Abc
                """)).isEqualTo("Abc Abc");
    }

    @Test
    void should_escape_html() {
        assertThat(markdownToHtml("Some <b>text</b>")).isEqualTo("Some &lt;b&gt;text&lt;/b&gt;");
    }

}