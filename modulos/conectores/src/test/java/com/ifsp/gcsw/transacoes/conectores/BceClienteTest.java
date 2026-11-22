package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.bce.BceCliente;
import com.ifsp.gcsw.transacoes.conectores.http.FonteIndisponivelException;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BceClienteTest extends BaseConectorTest {

  private static final Set<String> MOEDAS =
      Set.of("USD", "BRL", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "MXN", "EUR");
  private BceCliente cliente;

  @BeforeEach
  void preparar() {
    cliente = new BceCliente(cliente("BCE-" + System.nanoTime()), url(), validador());
  }

  private ResultadoColeta<CotacaoColetada> buscar() {
    return cliente.buscar(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-20"), MOEDAS);
  }

  @Test
  void sucesso_lerSdmxJsonReal() {
    wm.stubFor(
        get(urlPathMatching("/EXR/.*")).willReturn(okJson(fixture("bce/exr-diario-multi.json"))));

    ResultadoColeta<CotacaoColetada> r = buscar();

    assertThat(r.itens()).hasSize(45); // 9 moedas x 5 dias (ARS nao vem: encerrada)
    assertThat(r.itens()).allMatch(c -> c.base().equals("EUR") && c.fonte().equals("BCE"));
    CotacaoColetada audSegunda =
        r.itens().stream()
            .filter(c ->
                c.destino().equals("AUD")
                    && c.dataHora().toLocalDate().equals(LocalDate.parse("2026-11-16")))
            .findFirst()
            .orElseThrow();
    assertThat(audSegunda.valor()).isEqualByComparingTo("1.6301");
    assertThat(audSegunda.dataHora()).isEqualTo(OffsetDateTime.parse("2026-11-16T14:15+01:00"));
    wm.verify(
        getRequestedFor(urlPathEqualTo("/EXR/D.AUD+BRL+CAD+CHF+CNY+GBP+JPY+MXN+USD.EUR.SP00.A")));
  }

  @Test
  void corpoVazioNoFimDeSemana_coletaVazia() {
    wm.stubFor(
        get(urlPathMatching("/EXR/.*"))
            .willReturn(aResponse().withStatus(200).withBody(fixture("bce/exr-vazio-200.json"))));
    assertThat(buscar().itens()).isEmpty();
  }

  @Test
  void serieInexistente404_coletaVazia() {
    wm.stubFor(
        get(urlPathMatching("/EXR/.*"))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withBody(fixture("bce/exr-serie-inexistente-404.txt"))));
    assertThat(buscar().itens()).isEmpty();
  }

  @Test
  void sintaxeInvalida400_respostaInvalida() {
    wm.stubFor(get(urlPathMatching("/EXR/.*")).willReturn(aResponse().withStatus(400)));
    assertThatThrownBy(this::buscar).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void jsonMalformado() {
    wm.stubFor(
        get(urlPathMatching("/EXR/.*")).willReturn(okJson(fixture("bce/exr-malformado.json"))));
    assertThatThrownBy(this::buscar).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void jsonSemEstrutura() {
    wm.stubFor(get(urlPathMatching("/EXR/.*")).willReturn(okJson("{\"foo\":1}")));
    assertThatThrownBy(this::buscar).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void erro503_fonteIndisponivel() {
    wm.stubFor(get(urlPathMatching("/EXR/.*")).willReturn(aResponse().withStatus(503)));
    assertThatThrownBy(this::buscar).isInstanceOf(FonteIndisponivelException.class);
  }

  @Test
  void semMoedasAlemDoEuro_naoChamaAFonte() {
    assertThat(
            cliente
                .buscar(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-20"), Set.of("EUR"))
                .itens())
        .isEmpty();
    wm.verify(0, getRequestedFor(urlPathMatching("/EXR/.*")));
  }
}
