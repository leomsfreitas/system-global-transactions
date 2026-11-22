package com.ifsp.gcsw.transacoes.worker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.worker.execucao.ExecutorJobs;
import com.ifsp.gcsw.transacoes.worker.job.ResultadoJob;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Worker de ponta a ponta: conectores contra WireMock (respostas reais), PostgreSQL e Redis reais.
 */
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "worker.encerrar-apos-job=false",
      "conectores.tentativas=1",
      "conectores.timeout-leitura=2s",
      "logging.structured.format.console="
    })
@Import(JobsIntegracaoTest.Containers.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobsIntegracaoTest {

  private static final Path FIXTURES = Path.of("../conectores/src/test/resources/fixtures");

  @RegisterExtension
  static WireMockExtension wm =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @TestConfiguration(proxyBeanMethods = false)
  static class Containers {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
      return new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
          .withExposedPorts(6379);
    }
  }

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry r) {
    r.add("conectores.urls.bcb-ptax", () -> wm.baseUrl() + "/ptax");
    r.add("conectores.urls.bcb-sgs", () -> wm.baseUrl() + "/sgs");
    r.add("conectores.urls.bce", () -> wm.baseUrl() + "/bce");
    r.add("conectores.urls.fmi", () -> wm.baseUrl() + "/fmi");
    r.add("conectores.urls.banco-mundial", () -> wm.baseUrl() + "/bm");
  }

  @Autowired ExecutorJobs executor;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired MeterRegistry metricas;

  static String fixture(String caminho) {
    try {
      return Files.readString(FIXTURES.resolve(caminho));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeEach
  void stubs() {
    wm.resetAll();
    wm.stubFor(
        get(urlPathEqualTo("/ptax/Moedas")).willReturn(okJson(fixture("bcb/ptax-moedas.json"))));
    wm.stubFor(
        get(urlPathMatching("/ptax/CotacaoMoedaPeriodo.*"))
            .withQueryParam(
                "@moeda", com.github.tomakehurst.wiremock.client.WireMock.equalTo("'USD'"))
            .willReturn(okJson(fixture("bcb/ptax-cotacao-periodo-usd.json"))));
    wm.stubFor(
        get(urlPathMatching("/ptax/CotacaoMoedaPeriodo.*"))
            .atPriority(10)
            .willReturn(okJson(fixture("bcb/ptax-cotacao-dia-vazio.json"))));
    wm.stubFor(
        get(urlPathMatching("/bce/EXR/.*"))
            .willReturn(okJson(fixture("bce/exr-diario-multi.json"))));
    wm.stubFor(
        get(urlPathEqualTo("/sgs/dados/serie/bcdata.sgs.432/dados"))
            .willReturn(okJson(fixture("bcb/sgs-selic-meta-432.json"))));
    wm.stubFor(
        get(urlPathEqualTo("/sgs/dados/serie/bcdata.sgs.13522/dados"))
            .willReturn(okJson(fixture("bcb/sgs-ipca-12m-13522.json"))));
    wm.stubFor(
        get(urlPathMatching("/bm/.*/indicator/FP.CPI.TOTL.ZG"))
            .willReturn(okJson(fixture("bancomundial/inflacao-multi.json"))));
    wm.stubFor(
        get(urlPathMatching("/bm/.*/indicator/NY.GDP.MKTP.CD"))
            .willReturn(okJson(fixture("bancomundial/pib-multi.json"))));
    wm.stubFor(
        get(urlPathMatching("/bm/.*/indicator/GOV_WGI_PV.EST"))
            .willReturn(okJson(fixture("bancomundial/pv-est-multi.json"))));
    wm.stubFor(
        get(urlPathMatching("/fmi/.*/CPI/.*"))
            .willReturn(okJson(fixture("fmi/cpi-inflacao-anual-ar-br.json"))));
    wm.stubFor(
        get(urlPathMatching("/fmi/.*/MFS_IR/.*"))
            .willReturn(okJson(fixture("fmi/mfs-ir-juros-politica-mex-chn.json"))));
  }

  private int contar(String sql) {
    return jdbc.queryForObject(sql, Integer.class);
  }

  @Test
  @Order(1)
  void cotacoesBcb_gravaEReexecucaoNaoDuplica() {
    redis.opsForValue().set(ChavesCache.cotacaoAtual("USD", "BRL"), "{}");

    ResultadoJob primeira = executor.executar("AtualizacaoCotacoesBcbJob");
    ResultadoJob segunda = executor.executar("AtualizacaoCotacoesBcbJob");

    assertThat(primeira.gravados()).isEqualTo(20);
    assertThat(primeira.falhas()).isZero();
    assertThat(segunda.gravados()).isZero();
    assertThat(segunda.ignorados()).isEqualTo(20);
    assertThat(
            contar(
                "SELECT count(*) FROM cotacao c JOIN fonte_dados f ON f.id = c.fonte_id WHERE f.codigo = 'BCB'"))
        .isEqualTo(20);
    assertThat(redis.hasKey(ChavesCache.cotacaoAtual("USD", "BRL")))
        .as("cache invalidado")
        .isFalse();
    assertThat(
            metricas
                .counter(
                    "worker.job.registros", "job", "AtualizacaoCotacoesBcbJob", "tipo", "gravados")
                .count())
        .isEqualTo(20);
  }

  @Test
  @Order(2)
  void cotacoesBce_gravaParesEurX() {
    ResultadoJob r = executor.executar("AtualizacaoCotacoesBceJob");
    assertThat(r.gravados()).isEqualTo(45);
    BigDecimal eurBrl =
        jdbc.queryForObject(
            "SELECT c.valor FROM cotacao c JOIN moeda b ON b.id = c.moeda_base_id JOIN moeda d ON d.id = c.moeda_destino_id "
                + "WHERE b.codigo_iso = 'EUR' AND d.codigo_iso = 'BRL' ORDER BY c.data_hora DESC LIMIT 1",
            BigDecimal.class);
    assertThat(eurBrl).isPositive();
  }

  @Test
  @Order(3)
  void indicadores_todasAsFontes_eIdempotente() {
    ResultadoJob primeira = executor.executar("AtualizacaoIndicadoresJob");
    ResultadoJob segunda = executor.executar("AtualizacaoIndicadoresJob");

    assertThat(primeira.falhas()).isZero();
    assertThat(primeira.gravados()).isPositive();
    assertThat(segunda.gravados()).isZero();
    assertThat(
            contar("SELECT count(*) FROM indicador_economico WHERE tipo = 'ESTABILIDADE_POLITICA'"))
        .isEqualTo(14);
    assertThat(
            contar(
                "SELECT count(*) FROM indicador_economico i JOIN fonte_dados f ON f.id = i.fonte_id WHERE f.codigo = 'FMI'"))
        .isPositive();
  }

  @Test
  @Order(4)
  void indiceGeopolitico_calculaPaisesEAgregadoEu() {
    ResultadoJob r = executor.executar("CalculoIndiceGeopoliticoJob");
    ResultadoJob repetido = executor.executar("CalculoIndiceGeopoliticoJob");

    assertThat(r.gravados()).isEqualTo(15); // 14 paises + EU
    assertThat(repetido.gravados()).isZero();
    BigDecimal fatorUs =
        jdbc.queryForObject(
            "SELECT g.fator_ajuste FROM indice_geopolitico g JOIN pais p ON p.id = g.pais_id WHERE p.codigo_iso = 'US' ORDER BY g.data_referencia DESC LIMIT 1",
            BigDecimal.class);
    assertThat(fatorUs).isEqualByComparingTo("0.9820");
    BigDecimal indiceEu =
        jdbc.queryForObject(
            "SELECT g.indice_risco FROM indice_geopolitico g JOIN pais p ON p.id = g.pais_id WHERE p.codigo_iso = 'EU'",
            BigDecimal.class);
    // Media ponderada pelo PIB de DE, FR, IT, ES, NL com os dados reais (escopo-fontes.md secao 3)
    assertThat(indiceEu).isEqualByComparingTo("29.04");
  }

  @Test
  @Order(5)
  void falhaDeUmaFonte_naoDerrubaAsOutrasNemApagaDados() {
    int antes = contar("SELECT count(*) FROM indicador_economico");
    wm.stubFor(get(urlPathMatching("/fmi/.*")).willReturn(aResponse().withStatus(500)));
    wm.stubFor(
        get(urlPathEqualTo("/sgs/dados/serie/bcdata.sgs.13522/dados"))
            .willReturn(okJson("[{\"data\":\"01/11/2026\",\"valor\":\"4.70\"}]")));

    ResultadoJob r = executor.executar("AtualizacaoIndicadoresJob");

    assertThat(r.falhas()).isEqualTo(1);
    assertThat(r.gravados()).isEqualTo(1); // novo IPCA do BCB entrou mesmo com o FMI fora
    assertThat(contar("SELECT count(*) FROM indicador_economico")).isEqualTo(antes + 1);
  }

  @Test
  @Order(6)
  void fonteDesativada_naoEConsultada() {
    jdbc.update("UPDATE fonte_dados SET ativa = FALSE WHERE codigo = 'BCE'");
    try {
      ResultadoJob r = executor.executar("AtualizacaoCotacoesBceJob");
      assertThat(r).isEqualTo(ResultadoJob.VAZIO);
      wm.verify(
          0,
          com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
              urlPathMatching("/bce/.*")));
    } finally {
      jdbc.update("UPDATE fonte_dados SET ativa = TRUE WHERE codigo = 'BCE'");
    }
  }

  @Test
  @Order(7)
  void manutencaoDeParticoes_idempotente() {
    assertThat(executor.executar("ManutencaoParticoesJob").falhas()).isZero();
    assertThat(executor.executar("ManutencaoParticoesJob").falhas()).isZero();
    String proximo =
        java.time.YearMonth.now(java.time.ZoneOffset.UTC)
            .plusMonths(3)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
    assertThat(
            jdbc.queryForObject(
                "SELECT to_regclass('cotacao_" + proximo + "') IS NOT NULL", Boolean.class))
        .isTrue();
  }

  @Test
  @Order(8)
  void fonteDeCotacaoFora_jobRegistraFalha() {
    wm.stubFor(get(urlPathMatching("/bce/.*")).willReturn(aResponse().withStatus(503)));
    ResultadoJob r = executor.executar("AtualizacaoCotacoesBceJob");
    assertThat(r.falhas()).isEqualTo(1);
    assertThat(metricas.counter("worker.job.falhas", "job", "AtualizacaoCotacoesBceJob").count())
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  @Order(9)
  void jobDesconhecido() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> executor.executar("Inexistente"))
        .hasMessageContaining("Disponiveis");
    assertThat(executor.nomes()).contains("MigracaoJob", "CalculoIndiceGeopoliticoJob");
  }
}
