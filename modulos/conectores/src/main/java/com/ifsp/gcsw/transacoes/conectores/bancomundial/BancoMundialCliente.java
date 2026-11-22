package com.ifsp.gcsw.transacoes.conectores.bancomundial;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * API v2 do Banco Mundial (escopo-fontes.md secao 2.4). Erros chegam com HTTP 200 e corpo
 * [{"message": [...]}], por isso o corpo e inspecionado. O indicador de estabilidade politica e
 * configuravel: o codigo PV.EST foi substituido por GOV_WGI_PV.EST (ADR 0005).
 */
public class BancoMundialCliente implements FonteIndicadores {

  public static final String CODIGO = "BANCO_MUNDIAL";
  static final String INFLACAO = "FP.CPI.TOTL.ZG";
  static final String PIB = "NY.GDP.MKTP.CD";

  private final ClienteHttpResiliente http;
  private final String urlBase;
  private final String indicadorEstabilidade;
  private final ValidadorDados validador;
  private final Clock relogio;
  private final int anosHistorico;

  public BancoMundialCliente(
      ClienteHttpResiliente http,
      String urlBase,
      String indicadorEstabilidade,
      ValidadorDados validador,
      Clock relogio,
      int anosHistorico) {
    this.http = http;
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.indicadorEstabilidade = indicadorEstabilidade;
    this.validador = validador;
    this.relogio = relogio;
    this.anosHistorico = anosHistorico;
  }

  @Override
  public String codigo() {
    return CODIGO;
  }

  /** Inflacao e PIB. */
  @Override
  public ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises) {
    ResultadoColeta<IndicadorColetado> inflacao = indicador(paises, INFLACAO, "INFLACAO");
    ResultadoColeta<IndicadorColetado> pib = indicador(paises, PIB, "PIB");
    List<IndicadorColetado> itens = new ArrayList<>(inflacao.itens());
    itens.addAll(pib.itens());
    return new ResultadoColeta<>(itens, inflacao.descartados() + pib.descartados());
  }

  /** Estabilidade politica (WGI), exposta via {@link RiscoPoliticoBancoMundial}. */
  public ResultadoColeta<IndicadorColetado> estabilidadePolitica(List<PaisConsulta> paises) {
    return indicador(paises, indicadorEstabilidade, "ESTABILIDADE_POLITICA");
  }

  private ResultadoColeta<IndicadorColetado> indicador(
      List<PaisConsulta> paises, String codigo, String tipo) {
    List<PaisConsulta> comIso = paises.stream().filter(p -> p.iso3() != null).toList();
    if (comIso.isEmpty()) {
      return ResultadoColeta.vazio();
    }
    int anoFinal = LocalDate.now(relogio).getYear();
    String codigos = comIso.stream().map(PaisConsulta::iso2).collect(Collectors.joining(";"));
    URI uri =
        URI.create(
            urlBase
                + "/country/"
                + codigos
                + "/indicator/"
                + codigo
                + "?format=json&per_page=1000&date="
                + (anoFinal - anosHistorico)
                + ":"
                + anoFinal);
    RespostaHttp resposta = http.get(uri);
    if (!resposta.sucesso()) {
      throw new RespostaInvalidaException(
          "Banco Mundial respondeu HTTP " + resposta.status(), null);
    }
    JsonNode raiz = Json.ler(CODIGO, resposta.corpo());
    if (!raiz.isArray() || raiz.isEmpty()) {
      throw new RespostaInvalidaException("Banco Mundial devolveu formato inesperado", null);
    }
    if (raiz.path(0).has("message")) {
      throw new RespostaInvalidaException(
          "Banco Mundial recusou a consulta de "
              + codigo
              + ": "
              + raiz.path(0).path("message").toString(),
          null);
    }
    List<IndicadorColetado> itens = new ArrayList<>();
    int descartados = 0;
    for (JsonNode ponto : raiz.path(1)) {
      if (ponto.path("value").isNull()) {
        continue; // ano sem publicacao
      }
      String pais = ponto.path("country").path("id").asString("");
      BigDecimal valor = Json.decimal(ponto.get("value"));
      LocalDate periodo = ano(ponto.path("date").asString(""));
      boolean paisConhecido = comIso.stream().anyMatch(p -> p.iso2().equals(pais));
      if (paisConhecido && valor != null && periodo != null && validador.dataPlausivel(periodo)) {
        itens.add(new IndicadorColetado(pais, tipo, valor, periodo, CODIGO));
      } else {
        descartados++;
      }
    }
    return new ResultadoColeta<>(itens, descartados);
  }

  private static LocalDate ano(String texto) {
    try {
      return LocalDate.of(Integer.parseInt(texto), 1, 1);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
