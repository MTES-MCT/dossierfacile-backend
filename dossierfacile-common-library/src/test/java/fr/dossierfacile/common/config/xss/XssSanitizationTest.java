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
}
