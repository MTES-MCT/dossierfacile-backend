package fr.dossierfacile.common.model.ademe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdemeApiResultModelTest {
    @Test
    public void testParsing() throws Exception {
        String json1 = "{\"timestamp\":\"2026-05-05T09:14:24.080216838Z\"}";
        String json2 = "{\"timestamp\":\"2025-06-11T07:55:28.878Z\"}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        AdemeApiResultModel res1 = mapper.readValue(json1, AdemeApiResultModel.class);
        assertThat(res1).isNotNull();
        assertThat(res1.getTimestamp()).isNotNull();

        AdemeApiResultModel res2 = mapper.readValue(json2, AdemeApiResultModel.class);
        assertThat(res2).isNotNull();
        assertThat(res2.getTimestamp()).isNotNull();
    }
}
