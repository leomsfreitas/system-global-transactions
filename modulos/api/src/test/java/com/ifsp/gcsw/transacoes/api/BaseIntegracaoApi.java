package com.ifsp.gcsw.transacoes.api;

import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** Sobe a API inteira (porta aleatoria) com PostgreSQL e Redis reais e dados conhecidos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("teste")
@Import(ContainersApi.class)
public abstract class BaseIntegracaoApi {

  protected static final String CHAVE = "chave-teste";
  protected static final OffsetDateTime AGORA =
      OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

  @LocalServerPort protected int porta;
  @Autowired protected GravacaoDados gravacao;
  @Autowired protected StringRedisTemplate redis;

  protected RestClient cliente;

  @BeforeEach
  void prepararBase() {
    cliente = RestClient.builder().baseUrl("http://localhost:" + porta).build();
    redis.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
    // Exemplo canonico: USD->BRL 5,4210 e fator do BR 0,9850 (idempotente: ON CONFLICT DO NOTHING).
    OffsetDateTime cotacao = AGORA.truncatedTo(ChronoUnit.HOURS).minusHours(1);
    gravacao.inserirCotacao("USD", "BRL", new BigDecimal("5.4210"), cotacao, "BCB");
    gravacao.inserirCotacao("USD", "BRL", new BigDecimal("5.3900"), cotacao.minusDays(1), "BCB");
    gravacao.inserirCotacao("EUR", "CNY", new BigDecimal("7.8000"), cotacao, "BCE");
    gravacao.inserirCotacao("EUR", "MXN", new BigDecimal("19.5000"), cotacao, "BCE");
    gravacao.inserirCotacao("EUR", "JPY", new BigDecimal("172.4000"), cotacao, "BCE");
    gravacao.inserirCotacao("EUR", "BRL", new BigDecimal("6.2000"), cotacao, "BCE");
    gravacao.inserirCotacao("EUR", "USD", new BigDecimal("1.1400"), cotacao, "BCE");
    // Par desatualizado para testar 503
    gravacao.inserirCotacao("CHF", "BRL", new BigDecimal("6.5000"), cotacao.minusDays(30), "BCB");
    LocalDate hoje = AGORA.toLocalDate();
    gravacao.inserirIndice(
        "BR", new BigDecimal("30.00"), new BigDecimal("0.9850"), hoje, "BANCO_MUNDIAL");
    gravacao.inserirIndice(
        "US", new BigDecimal("35.98"), new BigDecimal("0.9820"), hoje, "BANCO_MUNDIAL");
    gravacao.inserirIndice(
        "JP", new BigDecimal("17.43"), new BigDecimal("0.9913"), hoje, "BANCO_MUNDIAL");
    gravacao.inserirIndice(
        "MX", new BigDecimal("40.35"), new BigDecimal("0.9798"), hoje, "BANCO_MUNDIAL");
    gravacao.inserirIndicador(
        "BR", TipoIndicador.INFLACAO, new BigDecimal("4.63"), LocalDate.parse("2026-10-01"), "BCB");
    gravacao.inserirIndicador(
        "BR", TipoIndicador.JUROS, new BigDecimal("13.75"), LocalDate.parse("2026-11-01"), "BCB");
    gravacao.inserirIndicador(
        "BR",
        TipoIndicador.PIB,
        new BigDecimal("2173665655937.27"),
        LocalDate.parse("2025-01-01"),
        "BANCO_MUNDIAL");
  }
}
