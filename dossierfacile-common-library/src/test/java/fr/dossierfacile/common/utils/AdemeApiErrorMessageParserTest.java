package fr.dossierfacile.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdemeApiErrorMessageParserTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldExtractMessageFromAdemeJsonBody() {
    String body =
        """
        {
          "correlationId": "237aa190-699c-11f1-880f-0283ab332d97",
          "success": false,
          "message": "Le DPE 2178V1001934U est introuvable.",
          "timestamp": "2026-06-16T15:57:55.687946058Z"
        }
        """;

    assertEquals(
        "Le DPE 2178V1001934U est introuvable.",
        AdemeApiErrorMessageParser.extractUserMessage(body, objectMapper));
  }

  @Test
  void shouldReturnRawBodyWhenJsonHasNoMessageField() {
    assertEquals("plain error", AdemeApiErrorMessageParser.extractUserMessage("plain error", objectMapper));
  }
}
