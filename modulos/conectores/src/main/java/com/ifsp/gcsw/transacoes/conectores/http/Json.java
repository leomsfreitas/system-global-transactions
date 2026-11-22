package com.ifsp.gcsw.transacoes.conectores.http;

import java.math.BigDecimal;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Leitura de JSON das fontes. Numeros sempre como BigDecimal (nunca double). */
public final class Json {

  private static final JsonMapper MAPPER =
      JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

  private Json() {}

  public static JsonNode ler(String fonte, String corpo) {
    try {
      return MAPPER.readTree(corpo);
    } catch (JacksonException e) {
      throw new RespostaInvalidaException("JSON invalido recebido da fonte " + fonte, e);
    }
  }

  /** Converte numero ou texto ("13.75") para BigDecimal; null se ausente ou invalido. */
  public static BigDecimal decimal(JsonNode no) {
    if (no == null || no.isNull() || no.isMissingNode()) {
      return null;
    }
    try {
      return no.isNumber() ? no.decimalValue() : new BigDecimal(no.asString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
