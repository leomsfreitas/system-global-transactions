package com.ifsp.gcsw.transacoes.conectores.fmi;

import com.ifsp.gcsw.transacoes.conectores.FonteIndicadores;
import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.Json;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaHttp;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.http.SdmxJson;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * FMI pelo SDMX 3.0 do portal novo (escopo-fontes.md secao 2.5; o portal antigo saiu do ar). Fonte
 * secundaria: inflacao anual (fluxo CPI) e taxa de politica monetaria mensal (fluxo MFS_IR).
 * Valores chegam como texto e as observacoes fora de ordem.
 */
public class FmiCliente implements FonteIndicadores {

  public static final String CODIGO = "FMI";
  private static final Pattern MENSAL = Pattern.compile("^(\\d{4})-M(\\d{2})$");
  private static final Pattern ANUAL = Pattern.compile("^(\\d{4})$");

  private final ClienteHttpResiliente http;
  private final String urlBase;
  private final ValidadorDados validador;
  private final Clock relogio;
  private final int anosHistorico;

  public FmiCliente(
      ClienteHttpResiliente http,
      String urlBase,
      ValidadorDados validador,
      Clock relogio,
      int anosHistorico) {
    this.http = http;
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.validador = validador;
    this.relogio = relogio;
    this.anosHistorico = anosHistorico;
  }

  @Override
  public String codigo() {
    return CODIGO;
  }

  @Override
  public ResultadoColeta<IndicadorColetado> buscar(List<PaisConsulta> paises) {
    List<PaisConsulta> comIso3 = paises.stream().filter(p -> p.iso3() != null).toList();
    if (comIso3.isEmpty()) {
      return ResultadoColeta.vazio();
    }
    Map<String, String> iso3ParaIso2 =
        comIso3.stream()
            .collect(Collectors.toMap(PaisConsulta::iso3, PaisConsulta::iso2, (a, b) -> a));
    String chaves = String.join("+", iso3ParaIso2.keySet());
    ResultadoColeta<IndicadorColetado> inflacao =
        fluxo("CPI", chaves + ".CPI._T.YOY_PCH_PA_PT.A", "INFLACAO", iso3ParaIso2::get);
    ResultadoColeta<IndicadorColetado> juros =
        fluxo("MFS_IR", chaves + ".MFS166_RT_PT_A_PT.M", "JUROS", iso3ParaIso2::get);
    List<IndicadorColetado> itens = new ArrayList<>(inflacao.itens());
    itens.addAll(juros.itens());
    return new ResultadoColeta<>(itens, inflacao.descartados() + juros.descartados());
  }

  private ResultadoColeta<IndicadorColetado> fluxo(
      String fluxo, String chave, String tipo, Function<String, String> iso2) {
    URI uri = URI.create(urlBase + "/data/dataflow/IMF.STA/" + fluxo + "/%2B/" + chave);
    RespostaHttp resposta = http.get(uri, Map.of("Accept", "application/json"));
    if (resposta.status() == 404 || (resposta.sucesso() && resposta.corpoVazio())) {
      return ResultadoColeta.vazio();
    }
    if (!resposta.sucesso()) {
      throw new RespostaInvalidaException("FMI respondeu HTTP " + resposta.status(), null);
    }
    JsonNode dados = Json.ler(CODIGO, resposta.corpo()).path("data");
    if (dados.isMissingNode()) {
      throw new RespostaInvalidaException("FMI devolveu formato inesperado", null);
    }
    int anoMinimo = LocalDate.now(relogio).getYear() - anosHistorico;
    List<IndicadorColetado> itens = new ArrayList<>();
    int descartados = 0;
    for (SdmxJson.Observacao obs :
        SdmxJson.ler(dados.path("structures").path(0), dados.path("dataSets").path(0))) {
      String pais = iso2.apply(obs.dimensoes().get("COUNTRY"));
      LocalDate periodo = periodo(obs.periodo());
      if (periodo != null && periodo.getYear() < anoMinimo) {
        continue; // fora da janela pedida (o FMI ignora startPeriod)
      }
      if (pais != null
          && obs.valor() != null
          && periodo != null
          && validador.dataPlausivel(periodo)) {
        itens.add(new IndicadorColetado(pais, tipo, obs.valor(), periodo, CODIGO));
      } else {
        descartados++;
      }
    }
    return new ResultadoColeta<>(itens, descartados);
  }

  static LocalDate periodo(String texto) {
    try {
      return interpretarPeriodo(texto);
    } catch (java.time.DateTimeException e) {
      return null;
    }
  }

  private static LocalDate interpretarPeriodo(String texto) {
    Matcher mensal = MENSAL.matcher(texto);
    if (mensal.matches()) {
      return LocalDate.of(Integer.parseInt(mensal.group(1)), Integer.parseInt(mensal.group(2)), 1);
    }
    Matcher anual = ANUAL.matcher(texto);
    if (anual.matches()) {
      return LocalDate.of(Integer.parseInt(anual.group(1)), 1, 1);
    }
    return null;
  }
}
