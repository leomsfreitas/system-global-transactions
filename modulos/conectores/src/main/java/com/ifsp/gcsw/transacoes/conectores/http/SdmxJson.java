package com.ifsp.gcsw.transacoes.conectores.http;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Leitor do formato SDMX-JSON usado pelo BCE (jsondata) e pelo FMI (SDMX 3.0). As chaves das series
 * ("0:3:0:0:0") apontam para posicoes nas listas de dimensoes, e as observacoes ("5": [valor, ...])
 * apontam para a lista de periodos. A ordem das observacoes nao e cronologica no FMI (AT01).
 */
public final class SdmxJson {

  private SdmxJson() {}

  public record Observacao(Map<String, String> dimensoes, String periodo, BigDecimal valor) {}

  /**
   * @param estrutura no com "dimensions" (series e observation)
   * @param conjunto no do dataSet com "series"
   */
  public static List<Observacao> ler(JsonNode estrutura, JsonNode conjunto) {
    List<Observacao> resultado = new ArrayList<>();
    JsonNode dimensoesSerie = estrutura.path("dimensions").path("series");
    JsonNode periodos = estrutura.path("dimensions").path("observation").path(0).path("values");
    JsonNode series = conjunto.path("series");
    if (!series.isObject()) {
      return resultado;
    }
    for (Map.Entry<String, JsonNode> serie : series.properties()) {
      String[] indices = serie.getKey().split(":");
      Map<String, String> dimensoes = new HashMap<>();
      for (int posicao = 0;
          posicao < indices.length && posicao < dimensoesSerie.size();
          posicao++) {
        JsonNode dimensao = dimensoesSerie.get(posicao);
        JsonNode valor = dimensao.path("values").path(Integer.parseInt(indices[posicao]));
        dimensoes.put(dimensao.path("id").asString(""), texto(valor));
      }
      JsonNode observacoes = serie.getValue().path("observations");
      for (Map.Entry<String, JsonNode> obs : observacoes.properties()) {
        String periodo = texto(periodos.path(Integer.parseInt(obs.getKey())));
        BigDecimal valor = Json.decimal(obs.getValue().path(0));
        resultado.add(new Observacao(dimensoes, periodo, valor));
      }
    }
    return resultado;
  }

  private static String texto(JsonNode valor) {
    if (valor.has("id")) {
      return valor.path("id").asString("");
    }
    return valor.path("value").asString("");
  }
}
