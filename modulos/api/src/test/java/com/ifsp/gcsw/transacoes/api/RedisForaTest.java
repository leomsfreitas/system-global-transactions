package com.ifsp.gcsw.transacoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Secao 6.4: se o Redis cair, a API continua servindo pelo PostgreSQL. Aqui o Redis nem existe. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.data.redis.host=127.0.0.1", "spring.data.redis.port=1"})
@ActiveProfiles("teste")
@Import(RedisForaTest.SoPostgres.class)
class RedisForaTest {

  @TestConfiguration(proxyBeanMethods = false)
  static class SoPostgres {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:17.10-alpine"));
    }
  }

  @LocalServerPort int porta;
  @Autowired GravacaoDados gravacao;

  @Test
  void apiRespondeSemRedis() {
    OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    gravacao.inserirCotacao("USD", "BRL", new BigDecimal("5.4210"), agora.minusHours(1), "BCB");
    gravacao.inserirIndice(
        "BR", new BigDecimal("30.00"), new BigDecimal("0.9850"), LocalDate.now(), "BANCO_MUNDIAL");
    RestClient cliente = RestClient.builder().baseUrl("http://localhost:" + porta).build();

    for (int i = 0; i < 3; i++) {
      String corpo =
          cliente
              .post()
              .uri("/api/v1/conversoes")
              .header("X-API-Key", "chave-teste")
              .contentType(MediaType.APPLICATION_JSON)
              .body("{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":100.00}")
              .retrieve()
              .body(String.class);
      assertThat(corpo).contains("\"valorConvertido\":533.97");
    }
    String moedas =
        cliente
            .get()
            .uri("/api/v1/moedas")
            .header("X-API-Key", "chave-teste")
            .retrieve()
            .body(String.class);
    assertThat(moedas).contains("USD");
  }
}
