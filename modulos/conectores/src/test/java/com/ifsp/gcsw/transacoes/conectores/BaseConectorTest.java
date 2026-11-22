package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ifsp.gcsw.transacoes.conectores.config.FabricaClientesHttp;
import com.ifsp.gcsw.transacoes.conectores.config.PropriedadesConectores;
import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.ValidadorDados;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

/** WireMock com as respostas reais gravadas na AT01 (src/test/resources/fixtures). */
public abstract class BaseConectorTest {

  /** "Hoje" dos testes: dia seguinte ao das fixtures gravadas. */
  protected static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-11-22T18:00:00Z"), ZoneOffset.UTC);

  @RegisterExtension
  protected static WireMockExtension wm =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  protected static PropriedadesConectores propriedades() {
    return new PropriedadesConectores(
        Duration.ofMillis(500),
        Duration.ofMillis(800),
        3,
        Duration.ofMillis(20),
        2.0,
        10,
        50f,
        Duration.ofSeconds(60),
        24,
        6,
        "GOV_WGI_PV.EST",
        new PropriedadesConectores.Urls("x", "x", "x", "x", "x"));
  }

  protected static ClienteHttpResiliente cliente(String nome) {
    PropriedadesConectores p = propriedades();
    return new FabricaClientesHttp(
            p,
            WebClient.builder(),
            FabricaClientesHttp.registroCircuitos(p),
            FabricaClientesHttp.registroRetries(p))
        .criar(nome);
  }

  protected static ValidadorDados validador() {
    return new ValidadorDados(RELOGIO);
  }

  protected static String url() {
    return wm.baseUrl();
  }

  protected static String fixture(String caminho) {
    try (InputStream in = BaseConectorTest.class.getResourceAsStream("/fixtures/" + caminho)) {
      if (in == null) {
        throw new IllegalArgumentException("Fixture inexistente: " + caminho);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
