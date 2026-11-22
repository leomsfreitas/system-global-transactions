package com.ifsp.gcsw.transacoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Teste de contrato (secao 6.6): chama a API real e valida requisicao e resposta contra
 * api-spec/openapi.yaml. Spec e codigo nao podem divergir.
 */
class ContratoApiTest extends BaseIntegracaoApi {

  private static OpenApiInteractionValidator validador;

  @BeforeAll
  static void carregarSpec() {
    String spec = Path.of("../../api-spec/openapi.yaml").toAbsolutePath().normalize().toString();
    validador = OpenApiInteractionValidator.createForSpecificationUrl(spec).build();
  }

  static Stream<Arguments> cenarios() {
    String hoje = AGORA.toLocalDate().toString();
    String semana = AGORA.toLocalDate().minusDays(7).toString();
    String canonico =
        "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":100.00,\"aplicarAjusteGeopolitico\":true}";
    return Stream.of(
        Arguments.of("POST", "/api/v1/conversoes", canonico, CHAVE, 200),
        Arguments.of(
            "POST",
            "/api/v1/conversoes",
            "{\"moedaOrigem\":\"usd\",\"moedaDestino\":\"jpy\",\"valor\":10}",
            CHAVE,
            200),
        Arguments.of(
            "POST",
            "/api/v1/conversoes",
            "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":0}",
            CHAVE,
            400),
        Arguments.of(
            "POST",
            "/api/v1/conversoes",
            "{\"moedaOrigem\":\"ARS\",\"moedaDestino\":\"BRL\",\"valor\":1}",
            CHAVE,
            404),
        Arguments.of(
            "POST",
            "/api/v1/conversoes",
            "{\"moedaOrigem\":\"CHF\",\"moedaDestino\":\"BRL\",\"valor\":1}",
            CHAVE,
            503),
        Arguments.of("POST", "/api/v1/conversoes", canonico, null, 401),
        Arguments.of("GET", "/api/v1/moedas", null, CHAVE, 200),
        Arguments.of("GET", "/api/v1/moedas", null, "errada", 401),
        Arguments.of("GET", "/api/v1/cotacoes/USD/BRL", null, CHAVE, 200),
        Arguments.of("GET", "/api/v1/cotacoes/cny/mxn", null, CHAVE, 200),
        Arguments.of("GET", "/api/v1/cotacoes/USDX/BRL", null, CHAVE, 400),
        Arguments.of("GET", "/api/v1/cotacoes/ARS/BRL", null, CHAVE, 404),
        Arguments.of("GET", "/api/v1/cotacoes/CHF/BRL", null, CHAVE, 503),
        Arguments.of(
            "GET",
            "/api/v1/cotacoes/USD/BRL/historico?dataInicio=" + semana + "&dataFim=" + hoje,
            null,
            CHAVE,
            200),
        Arguments.of(
            "GET",
            "/api/v1/cotacoes/USD/BRL/historico?dataInicio=" + hoje + "&dataFim=" + semana,
            null,
            CHAVE,
            400),
        Arguments.of(
            "GET",
            "/api/v1/cotacoes/USD/BRL/historico?dataInicio=2001-01-01&dataFim=2001-01-31",
            null,
            CHAVE,
            404),
        Arguments.of("GET", "/api/v1/indicadores/BR", null, CHAVE, 200),
        Arguments.of("GET", "/api/v1/indicadores/ZZ", null, CHAVE, 404),
        Arguments.of("GET", "/api/v1/indicadores/BRA", null, CHAVE, 400),
        Arguments.of("GET", "/api/v1/indices-geopoliticos/br", null, CHAVE, 200),
        Arguments.of("GET", "/api/v1/indices-geopoliticos/FR", null, CHAVE, 404));
  }

  @ParameterizedTest(name = "{0} {1} -> {4}")
  @MethodSource("cenarios")
  void respostaBateComASpec(
      String metodo, String caminho, String corpo, String chave, int statusEsperado) {
    ResponseEntity<String> resposta = chamar(metodo, caminho, corpo, chave);
    assertThat(resposta.getStatusCode().value()).isEqualTo(statusEsperado);

    String semQuery = caminho.contains("?") ? caminho.substring(0, caminho.indexOf('?')) : caminho;
    SimpleResponse.Builder respostaSimples =
        SimpleResponse.Builder.status(resposta.getStatusCode().value())
            .withBody(resposta.getBody());
    resposta.getHeaders().forEach((nome, valores) -> respostaSimples.withHeader(nome, valores));
    Request.Method verbo = Request.Method.valueOf(metodo);

    ValidationReport relatorio;
    if (chave == null || statusEsperado == 401 || statusEsperado == 400) {
      // Requisicoes propositalmente invalidas: so a resposta precisa obedecer ao contrato.
      relatorio = validador.validateResponse(semQuery, verbo, respostaSimples.build());
    } else {
      SimpleRequest.Builder requisicao =
          new SimpleRequest.Builder(metodo, semQuery).withHeader("X-API-Key", chave);
      if (caminho.contains("?")) {
        for (String par : caminho.substring(caminho.indexOf('?') + 1).split("&")) {
          String[] kv = par.split("=");
          requisicao.withQueryParam(kv[0], kv[1]);
        }
      }
      if (corpo != null) {
        requisicao.withContentType("application/json").withBody(corpo);
      }
      relatorio = validador.validate(requisicao.build(), respostaSimples.build());
    }
    assertThat(relatorio.getMessages()).as(relatorio.toString()).isEmpty();
  }

  private ResponseEntity<String> chamar(String metodo, String caminho, String corpo, String chave) {
    var req = cliente.method(org.springframework.http.HttpMethod.valueOf(metodo)).uri(caminho);
    if (chave != null) {
      req = req.header("X-API-Key", chave);
    }
    if (corpo != null) {
      req = req.contentType(MediaType.APPLICATION_JSON).body(corpo);
    }
    return req.retrieve().onStatus(HttpStatusCode::isError, (r, s) -> {}).toEntity(String.class);
  }

  @SuppressWarnings("unused")
  private static List<String> naoUsado() {
    return List.of();
  }
}
