package com.ifsp.gcsw.transacoes.conectores.bcb;

import com.ifsp.gcsw.transacoes.conectores.FonteIndicadores;
import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.Json;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaHttp;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;

/**
 * Selic e IPCA do Brasil pelo SGS (escopo-fontes.md secao 2.2). Sempre com dataInicial/dataFinal: o
 * atalho /ultimos/N devolveu HTTP 304 sem corpo na serie 433 (risco R09).
 *
 * <ul>
 *   <li>INFLACAO: serie 13522, IPCA acumulado em 12 meses, mensal.
 *   <li>JUROS: serie 432, meta Selic diaria; grava o ultimo valor de cada mes. Datas futuras (a
 *       meta vale ate a proxima reuniao do Copom) sao descartadas.
 * </ul>
 */
public class BcbSgsCliente implements FonteIndicadores {

  public static final String CODIGO = "BCB";
  static final int SERIE_IPCA_12M = 13522;
  static final int SERIE_SELIC_META = 432;
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ClienteHttpResiliente http;
  private final String urlBase;
  private final ValidadorDados validador;
  private final Clock relogio;
  private final int mesesHistorico;

  public BcbSgsCliente(
      ClienteHttpResiliente http,
      String urlBase,
      ValidadorDados validador,
      Clock relogio,
      int mesesHistorico) {
    this.http = http;
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.validador = validador;
    this.relogio = relogio;
    this.mesesHistorico = mesesHistorico;
  }

  @Override
  public String codigo() {
    return CODIGO;
  }

  @Override
  public ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises) {
    if (paises.stream().noneMatch(p -> "BR".equals(p.iso2()))) {
      return ResultadoColeta.vazio();
    }
    LocalDate fim = LocalDate.now(relogio);
    LocalDate inicio = fim.minusMonths(mesesHistorico).withDayOfMonth(1);
    List<IndicadorColetado> itens = new ArrayList<>();
    int descartados = 0;

    Map<LocalDate, BigDecimal> ultimoPorMes = new TreeMap<>();
    for (JsonNode ponto : serie(SERIE_SELIC_META, inicio, fim)) {
      LocalDate data = data(ponto);
      BigDecimal valor = Json.decimal(ponto.get("valor"));
      if (data == null || valor == null || data.isAfter(fim) || !validador.dataPlausivel(data)) {
        descartados++;
        continue;
      }
      ultimoPorMes.put(data.withDayOfMonth(1), valor);
    }
    ultimoPorMes.forEach(
        (mes, valor) -> itens.add(new IndicadorColetado("BR", "JUROS", valor, mes, CODIGO)));

    for (JsonNode ponto : serie(SERIE_IPCA_12M, inicio, fim)) {
      LocalDate data = data(ponto);
      BigDecimal valor = Json.decimal(ponto.get("valor"));
      if (data == null || valor == null || !validador.dataPlausivel(data)) {
        descartados++;
        continue;
      }
      itens.add(new IndicadorColetado("BR", "INFLACAO", valor, data.withDayOfMonth(1), CODIGO));
    }
    return new ResultadoColeta<>(itens, descartados);
  }

  private JsonNode serie(int codigo, LocalDate inicio, LocalDate fim) {
    URI uri =
        URI.create(
            urlBase
                + "/dados/serie/bcdata.sgs."
                + codigo
                + "/dados?formato=json&dataInicial="
                + DATA.format(inicio)
                + "&dataFinal="
                + DATA.format(fim));
    RespostaHttp resposta = http.get(uri);
    if (resposta.status() == 404) {
      throw new RespostaInvalidaException("Serie SGS " + codigo + " nao existe", null);
    }
    if (!resposta.sucesso() || resposta.corpoVazio()) {
      throw new RespostaInvalidaException(
          "SGS respondeu HTTP " + resposta.status() + " sem dados", null);
    }
    JsonNode raiz = Json.ler(CODIGO, resposta.corpo());
    if (!raiz.isArray()) {
      throw new RespostaInvalidaException("SGS devolveu formato inesperado", null);
    }
    return raiz;
  }

  private static LocalDate data(JsonNode ponto) {
    try {
      return LocalDate.parse(ponto.path("data").asString(""), DATA);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
