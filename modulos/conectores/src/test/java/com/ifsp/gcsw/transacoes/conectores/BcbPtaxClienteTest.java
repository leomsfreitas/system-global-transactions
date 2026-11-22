package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.bcb.BcbPtaxCliente;
import com.ifsp.gcsw.transacoes.conectores.http.FonteIndisponivelException;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BcbPtaxClienteTest extends BaseConectorTest {

  private static final LocalDate INICIO = LocalDate.parse("2026-11-16");
  private static final LocalDate FIM = LocalDate.parse("2026-11-20");
  private BcbPtaxCliente cliente;

  @BeforeEach
  void preparar() {
    cliente = new BcbPtaxCliente(cliente("BCB-PTAX-" + System.nanoTime()), url(), validador());
    wm.stubFor(get(urlPathEqualTo("/Moedas")).willReturn(okJson(fixture("bcb/ptax-moedas.json"))));
  }

  @Test
  void sucesso_lerBoletinsReaisDoPeriodo() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(okJson(fixture("bcb/ptax-cotacao-periodo-usd.json"))));

    ResultadoColeta<CotacaoColetada> r = cliente.buscar(INICIO, FIM, Set.of("USD"));

    assertThat(r.itens()).hasSize(20);
    assertThat(r.descartados()).isZero();
    CotacaoColetada ultima = r.itens().getLast();
    assertThat(ultima.base()).isEqualTo("USD");
    assertThat(ultima.destino()).isEqualTo("BRL");
    assertThat(ultima.valor()).isEqualByComparingTo("5.88890");
    // 2026-11-19 13:04:18.325373 em Brasilia (UTC-3)
    assertThat(ultima.dataHora())
        .isEqualTo(OffsetDateTime.parse("2026-11-19T13:04:18.325373-03:00"));
    assertThat(ultima.fonte()).isEqualTo("BCB");
    wm.verify(
        getRequestedFor(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .withQueryParam(
                "@moeda", com.github.tomakehurst.wiremock.client.WireMock.equalTo("'USD'")));
  }

  @Test
  void consultaSoMoedasQueOPtaxPublica() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(okJson(fixture("bcb/ptax-cotacao-dia-vazio.json"))));

    cliente.buscar(INICIO, FIM, Set.of("BRL", "CNY", "MXN", "EUR"));

    // CNY e MXN nao estao em /Moedas; BRL e a propria moeda pivo.
    wm.verify(1, getRequestedFor(urlPathMatching("/CotacaoMoedaPeriodo.*")));
  }

  @Test
  void vazioComHttp200_naoEErro() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(okJson(fixture("bcb/ptax-cotacao-dia-vazio.json"))));
    ResultadoColeta<CotacaoColetada> r = cliente.buscar(INICIO, FIM, Set.of("USD"));
    assertThat(r.itens()).isEmpty();
    assertThat(r.descartados()).isZero();
  }

  @Test
  void dadoInvalidoEDescartadoEContado() {
    String corpo =
        """
        {"value":[
          {"cotacaoVenda":5.5,"dataHoraCotacao":"2026-11-19 13:04:18.1"},
          {"cotacaoVenda":-1,"dataHoraCotacao":"2026-11-19 13:04:18.1"},
          {"cotacaoVenda":5.5,"dataHoraCotacao":"2099-01-01 10:00:00"},
          {"cotacaoVenda":5.5,"dataHoraCotacao":"data ruim"},
          {"dataHoraCotacao":"2026-11-19 13:04:18"}
        ]}\
        """;
    wm.stubFor(get(urlPathMatching("/CotacaoMoedaPeriodo.*")).willReturn(okJson(corpo)));

    ResultadoColeta<CotacaoColetada> r = cliente.buscar(INICIO, FIM, Set.of("USD"));

    assertThat(r.itens()).hasSize(1);
    assertThat(r.descartados()).isEqualTo(4);
    assertThat(r.lidos()).isEqualTo(5);
  }

  @Test
  void erro500_tentaTresVezesEFalha() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(aResponse().withStatus(500).withBody(fixture("bcb/ptax-erro-500.html"))));

    assertThatThrownBy(() -> cliente.buscar(INICIO, FIM, Set.of("USD")))
        .isInstanceOf(FonteIndisponivelException.class);
    wm.verify(3, getRequestedFor(urlPathMatching("/CotacaoMoedaPeriodo.*")));
  }

  @Test
  void timeout_viraFonteIndisponivel() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(okJson(fixture("bcb/ptax-cotacao-periodo-usd.json")).withFixedDelay(3000)));

    assertThatThrownBy(() -> cliente.buscar(INICIO, FIM, Set.of("USD")))
        .isInstanceOf(FonteIndisponivelException.class);
  }

  @Test
  void jsonMalformado_respostaInvalida() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*"))
            .willReturn(okJson(fixture("bcb/ptax-malformado.json"))));
    assertThatThrownBy(() -> cliente.buscar(INICIO, FIM, Set.of("USD")))
        .isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void erro4xx_naoRepeteEFalha() {
    wm.stubFor(
        get(urlPathMatching("/CotacaoMoedaPeriodo.*")).willReturn(aResponse().withStatus(400)));
    assertThatThrownBy(() -> cliente.buscar(INICIO, FIM, Set.of("USD")))
        .isInstanceOf(RespostaInvalidaException.class);
    wm.verify(1, getRequestedFor(urlPathMatching("/CotacaoMoedaPeriodo.*")));
  }

  @Test
  void moedasPublicadas_lidasDoEndpointReal() {
    assertThat(cliente.moedasPublicadas())
        .containsExactly("AUD", "CAD", "CHF", "DKK", "EUR", "GBP", "JPY", "NOK", "SEK", "USD");
    assertThat(new BigDecimal("1")).isNotNull();
  }
}
