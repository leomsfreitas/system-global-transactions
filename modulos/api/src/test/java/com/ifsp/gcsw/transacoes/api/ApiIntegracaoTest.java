package com.ifsp.gcsw.transacoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ApiIntegracaoTest extends BaseIntegracaoApi {

  @Autowired JsonMapper json;
  @Autowired JdbcTemplate jdbc;

  private ResponseEntity<String> post(String corpo, String chave) {
    var req =
        cliente
            .post()
            .uri("/api/v1/conversoes")
            .contentType(MediaType.APPLICATION_JSON)
            .body(corpo);
    if (chave != null) {
      req = req.header("X-API-Key", chave);
    }
    return req.retrieve().onStatus(HttpStatusCode::isError, (r, s) -> {}).toEntity(String.class);
  }

  private ResponseEntity<String> get(String caminho) {
    return cliente
        .get()
        .uri(caminho)
        .header("X-API-Key", CHAVE)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (r, s) -> {})
        .toEntity(String.class);
  }

  @Test
  void exemploCanonico_viaHttp_retorna533_97_eGravaAuditoria() {
    Integer antes = jdbc.queryForObject("SELECT count(*) FROM conversao", Integer.class);

    ResponseEntity<String> r =
        post(
            "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":100.00,\"aplicarAjusteGeopolitico\":true}",
            CHAVE);

    assertThat(r.getStatusCode().value()).isEqualTo(200);
    JsonNode corpo = json.readTree(r.getBody());
    assertThat(corpo.get("valorConvertido").decimalValue()).isEqualByComparingTo("533.97");
    assertThat(corpo.get("taxaCambio").decimalValue()).isEqualByComparingTo("5.4210");
    assertThat(corpo.get("fatorGeopolitico").decimalValue()).isEqualByComparingTo("0.9850");
    assertThat(r.getBody())
        .contains("\"valorConvertido\":533.97")
        .contains("\"fatorGeopolitico\":0.9850");
    assertThat(r.getHeaders().getFirst("X-Data-Referencia")).isNotBlank();
    assertThat(r.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM conversao", Integer.class))
        .isEqualTo(antes + 1);
    String prefixo =
        jdbc.queryForObject(
            "SELECT api_key_hash_prefixo FROM conversao ORDER BY id DESC LIMIT 1", String.class);
    assertThat(prefixo).isEqualTo("c9648706");
  }

  @Test
  void ajusteDesligado_fatorUm() {
    ResponseEntity<String> r =
        post(
            "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":100.00,\"aplicarAjusteGeopolitico\":false}",
            CHAVE);
    JsonNode corpo = json.readTree(r.getBody());
    // Escala exata no JSON: 1.0000 e 542.10 (o JsonNode le como double, por isso a checagem
    // textual)
    assertThat(r.getBody())
        .contains("\"fatorGeopolitico\":1.0000")
        .contains("\"valorConvertido\":542.10");
  }

  @Test
  void moedaComZeroCasas_JPY_viaTaxaCruzada() {
    ResponseEntity<String> r =
        post("{\"moedaOrigem\":\"BRL\",\"moedaDestino\":\"JPY\",\"valor\":100}", CHAVE);
    assertThat(r.getStatusCode().value()).isEqualTo(200);
    JsonNode corpo = json.readTree(r.getBody());
    // Inverso de EUR->BRL x EUR->JPY via BCE: 172.4 / 6.2 = 27.806452; 100 x 27.806452 x 0.9913 =
    // 2756.45 -> 2756
    assertThat(r.getBody()).contains("\"valorConvertido\":2756,");
  }

  @Test
  void minusculasSaoNormalizadas() {
    ResponseEntity<String> r = get("/api/v1/cotacoes/usd/brl");
    assertThat(r.getStatusCode().value()).isEqualTo(200);
    assertThat(json.readTree(r.getBody()).get("base").asString()).isEqualTo("USD");
  }

  @Test
  void validacoesRetornam400ComMensagemEmPortugues() {
    ResponseEntity<String> r = post("{\"moedaOrigem\":\"US\",\"valor\":-1}", CHAVE);
    assertThat(r.getStatusCode().value()).isEqualTo(400);
    JsonNode erro = json.readTree(r.getBody());
    assertThat(erro.get("codigo").asInt()).isEqualTo(400);
    assertThat(erro.get("mensagem").asString())
        .contains("moedaDestino: e obrigatorio")
        .contains("moedaOrigem")
        .contains("valor");
    assertThat(erro.get("dataHora").asString()).isNotBlank();

    assertThat(
            post(
                    "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":1,\"extra\":1}",
                    CHAVE)
                .getStatusCode()
                .value())
        .isEqualTo(400);
    assertThat(post("nao e json", CHAVE).getStatusCode().value()).isEqualTo(400);
    assertThat(
            post(
                    "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":1000000000001}",
                    CHAVE)
                .getStatusCode()
                .value())
        .isEqualTo(400);
    assertThat(
            post("{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":1.1234567}", CHAVE)
                .getStatusCode()
                .value())
        .isEqualTo(400);
    assertThat(
            get("/api/v1/cotacoes/USD/BRL/historico?dataInicio=2026-01-01").getStatusCode().value())
        .isEqualTo(400);
    assertThat(
            get("/api/v1/cotacoes/USD/BRL/historico?dataInicio=ontem&dataFim=2026-01-01")
                .getStatusCode()
                .value())
        .isEqualTo(400);
    assertThat(
            get("/api/v1/cotacoes/USD/BRL/historico?dataInicio=2024-01-01&dataFim=2026-01-01")
                .getBody())
        .contains("366");
  }

  @Test
  void chaveAusenteOuInvalida_401_semVazarDetalhes() {
    ResponseEntity<String> semChave = post("{}", null);
    assertThat(semChave.getStatusCode().value()).isEqualTo(401);
    assertThat(semChave.getBody()).contains("Chave de API ausente ou invalida");
    ResponseEntity<String> invalida =
        cliente
            .get()
            .uri("/api/v1/moedas")
            .header("X-API-Key", "outra")
            .retrieve()
            .onStatus(HttpStatusCode::isError, (r, s) -> {})
            .toEntity(String.class);
    assertThat(invalida.getStatusCode().value()).isEqualTo(401);
  }

  @Test
  void dadoDesatualizado_503() {
    ResponseEntity<String> r = get("/api/v1/cotacoes/CHF/BRL");
    assertThat(r.getStatusCode().value()).isEqualTo(503);
    assertThat(r.getBody()).contains("mais antiga que o limite de 4 dias");
  }

  @Test
  void rotaInexistente_404_e_metodoErrado_405() {
    assertThat(get("/api/v1/nada").getStatusCode().value()).isEqualTo(404);
    ResponseEntity<String> r =
        cliente
            .delete()
            .uri("/api/v1/moedas")
            .header("X-API-Key", CHAVE)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (x, y) -> {})
            .toEntity(String.class);
    assertThat(r.getStatusCode().value()).isEqualTo(405);
  }

  @Test
  void cabecalhosDeSeguranca() {
    ResponseEntity<String> r = get("/api/v1/moedas");
    assertThat(r.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(r.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
    assertThat(r.getHeaders().getFirst("Content-Security-Policy")).contains("default-src 'none'");
  }

  @Test
  void healthPublicoComProbes() {
    for (String caminho :
        new String[] {
          "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
        }) {
      ResponseEntity<String> r = cliente.get().uri(caminho).retrieve().toEntity(String.class);
      assertThat(r.getStatusCode().value()).isEqualTo(200);
      assertThat(r.getBody()).contains("UP");
    }
  }

  @Test
  void swaggerUiServeASpecEscritaAMao() {
    ResponseEntity<String> spec =
        cliente.get().uri("/api-spec/openapi.yaml").retrieve().toEntity(String.class);
    assertThat(spec.getBody()).contains("openapi: 3.0.3").contains("/api/v1/conversoes");
    ResponseEntity<String> ui =
        cliente
            .get()
            .uri("/swagger-ui/index.html")
            .retrieve()
            .onStatus(HttpStatusCode::isError, (x, y) -> {})
            .toEntity(String.class);
    assertThat(ui.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  void respostasDeErroNaoTemStackTrace() {
    Map<String, String> casos =
        Map.of(
            "/api/v1/cotacoes/ARS/BRL", "Moeda nao suportada: ARS",
            "/api/v1/indices-geopoliticos/ZZ", "Pais nao encontrado: ZZ");
    casos.forEach((caminho, mensagem) -> {
      String corpo = get(caminho).getBody();
      assertThat(corpo).contains(mensagem).doesNotContain("Exception").doesNotContain("at com.");
    });
  }
}
