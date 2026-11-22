package com.ifsp.gcsw.transacoes.conectores.bcb;

import com.ifsp.gcsw.transacoes.conectores.FonteCotacoes;
import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.Json;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaHttp;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;

/**
 * Cotacoes PTAX do BCB pela API Olinda (AT01, escopo-fontes.md secao 2.1). Grava o par X -> BRL com
 * cotacaoVenda, um registro por boletim. Vazio com HTTP 200 e normal (fim de semana, feriado, moeda
 * nao publicada).
 */
public class BcbPtaxCliente implements FonteCotacoes {

  public static final String CODIGO = "BCB";
  private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter DATA_PARAMETRO = DateTimeFormatter.ofPattern("MM-dd-yyyy");
  private static final DateTimeFormatter DATA_HORA_BOLETIM =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd HH:mm:ss")
          .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
          .optionalEnd()
          .toFormatter();

  private final ClienteHttpResiliente http;
  private final String urlBase;
  private final ValidadorDados validador;

  public BcbPtaxCliente(ClienteHttpResiliente http, String urlBase, ValidadorDados validador) {
    this.http = http;
    this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    this.validador = validador;
  }

  @Override
  public String codigo() {
    return CODIGO;
  }

  /** Moedas que o PTAX publica (endpoint Moedas). */
  public Set<String> moedasPublicadas() {
    RespostaHttp resposta = http.get(URI.create(urlBase + "/Moedas?%24format=json"));
    exigirSucesso(resposta);
    Set<String> moedas = new TreeSet<>();
    for (JsonNode moeda : Json.ler(CODIGO, resposta.corpo()).path("value")) {
      String simbolo = moeda.path("simbolo").asString("");
      if (ValidadorDados.codigoMoedaValido(simbolo)) {
        moedas.add(simbolo);
      }
    }
    return moedas;
  }

  @Override
  public ResultadoColeta<CotacaoColetada> buscar(
      LocalDate inicio, LocalDate fim, Set<String> moedas) {
    Set<String> alvo = new HashSet<>(moedas);
    alvo.remove("BRL");
    alvo.retainAll(moedasPublicadas());
    List<CotacaoColetada> itens = new ArrayList<>();
    int descartados = 0;
    for (String moeda : new TreeSet<>(alvo)) {
      URI uri =
          URI.create(
              urlBase
                  + "/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)"
                  + "?@moeda=%27"
                  + moeda
                  + "%27&@dataInicial=%27"
                  + DATA_PARAMETRO.format(inicio)
                  + "%27&@dataFinalCotacao=%27"
                  + DATA_PARAMETRO.format(fim)
                  + "%27&%24format=json");
      RespostaHttp resposta = http.get(uri);
      exigirSucesso(resposta);
      for (JsonNode boletim : Json.ler(CODIGO, resposta.corpo()).path("value")) {
        BigDecimal valor = Json.decimal(boletim.get("cotacaoVenda"));
        OffsetDateTime dataHora = dataHora(boletim.path("dataHoraCotacao").asString(""));
        if (validador.cotacaoValida(valor, dataHora)) {
          itens.add(new CotacaoColetada(moeda, "BRL", valor, dataHora, CODIGO));
        } else {
          descartados++;
        }
      }
    }
    return new ResultadoColeta<>(itens, descartados);
  }

  private static OffsetDateTime dataHora(String texto) {
    try {
      return LocalDateTime.parse(texto, DATA_HORA_BOLETIM).atZone(BRASILIA).toOffsetDateTime();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static void exigirSucesso(RespostaHttp resposta) {
    if (!resposta.sucesso()) {
      throw new RespostaInvalidaException("PTAX respondeu HTTP " + resposta.status(), null);
    }
  }
}
