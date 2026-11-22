package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.http.ClienteHttpResiliente;
import com.ifsp.gcsw.transacoes.conectores.http.FonteIndisponivelException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class CircuitoTest extends BaseConectorTest {

  @Test
  void circuitoAbreAposFalhasSeguidas_eParaDeChamarAFonte() {
    wm.stubFor(get(urlPathMatching("/falha")).willReturn(aResponse().withStatus(500)));
    ClienteHttpResiliente http = cliente("CIRCUITO-" + System.nanoTime());
    URI uri = URI.create(url() + "/falha");

    // 2 chamadas x 3 tentativas = 6 falhas registradas (minimo de 5 chamadas, 50% de falha)
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> http.get(uri)).isInstanceOf(FonteIndisponivelException.class);
    }
    int antes = wm.findAll(getRequestedFor(urlPathMatching("/falha"))).size();

    assertThatThrownBy(() -> http.get(uri))
        .isInstanceOf(FonteIndisponivelException.class)
        .hasMessageContaining("Circuito aberto");
    wm.verify(antes, getRequestedFor(urlPathMatching("/falha")));
  }
}
