package com.ifsp.gcsw.transacoes.conectores.bce;

import com.ifsp.gcsw.transacoes.conectores.FonteCotacoes;
import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.Json;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaHttp;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.http.SdmxJson;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/**
 * Taxas de referencia diarias do BCE (EXR, escopo-fontes.md secao 2.3). Uma chamada com todas as
 * moedas. Grava o par EUR -> X; o instante e o da taxa de referencia (14h15 de Frankfurt). Fim de
 * semana: HTTP 200 com corpo vazio. Serie inexistente: 404. Ambos viram coleta vazia.
 */
public class BceCliente implements FonteCotacoes {

  public static final String CODIGO = "BCE";
  private static final ZoneId FRANKFURT = ZoneId.of("Europe/Berlin");
  private static final LocalTime HORARIO_REFERENCIA = LocalTime.of(14, 15);

  private final ClienteHttpResiliente http;
  private final String urlBase;
  private final ValidadorDados validador;

  public BceCliente(ClienteHttpResiliente http, String urlBase, ValidadorDados validador) {
    this.http = http;
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.validador = validador;
  }

  @Override
  public String codigo() {
    return CODIGO;
  }

  @Override
  public ResultadoColeta<CotacaoColetada> buscar(
      LocalDate inicio, LocalDate fim, Set<String> moedas) {
    Set<String> alvo = new TreeSet<>(moedas);
    alvo.remove("EUR");
    alvo.removeIf(m -> !ValidadorDados.codigoMoedaValido(m));
    if (alvo.isEmpty()) {
      return ResultadoColeta.vazio();
    }
    URI uri =
        URI.create(
            urlBase
                + "/EXR/D."
                + String.join("+", alvo)
                + ".EUR.SP00.A?startPeriod="
                + inicio
                + "&endPeriod="
                + fim
                + "&format=jsondata");
    RespostaHttp resposta = http.get(uri, Map.of("Accept", "application/json"));
    if (resposta.status() == 404 || (resposta.sucesso() && resposta.corpoVazio())) {
      return ResultadoColeta.vazio();
    }
    if (!resposta.sucesso()) {
      throw new RespostaInvalidaException("BCE respondeu HTTP " + resposta.status(), null);
    }
    JsonNode raiz = Json.ler(CODIGO, resposta.corpo());
    if (!raiz.has("structure") || !raiz.has("dataSets")) {
      throw new RespostaInvalidaException("BCE devolveu formato inesperado", null);
    }
    List<CotacaoColetada> itens = new ArrayList<>();
    int descartados = 0;
    for (SdmxJson.Observacao obs :
        SdmxJson.ler(raiz.path("structure"), raiz.path("dataSets").path(0))) {
      if (obs.valor() == null) {
        continue; // dia sem publicacao para a moeda: nao e dado invalido
      }
      String moeda = obs.dimensoes().get("CURRENCY");
      OffsetDateTime dataHora = dataHora(obs.periodo());
      if (alvo.contains(moeda) && validador.cotacaoValida(obs.valor(), dataHora)) {
        itens.add(new CotacaoColetada("EUR", moeda, obs.valor(), dataHora, CODIGO));
      } else {
        descartados++;
      }
    }
    return new ResultadoColeta<>(itens, descartados);
  }

  private static OffsetDateTime dataHora(String periodo) {
    try {
      return LocalDate.parse(periodo)
          .atTime(HORARIO_REFERENCIA)
          .atZone(FRANKFURT)
          .toOffsetDateTime();
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
