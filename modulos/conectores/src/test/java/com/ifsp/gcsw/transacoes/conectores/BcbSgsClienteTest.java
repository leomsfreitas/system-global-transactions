package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.bcb.BcbSgsCliente;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BcbSgsClienteTest extends BaseConectorTest {

  private static final List<PaisConsulta> BR = List.of(new PaisConsulta("BR", "BRA"));
  private BcbSgsCliente cliente;

  @BeforeEach
  void preparar() {
    cliente =
        new BcbSgsCliente(cliente("SGS-" + System.nanoTime()), url(), validador(), RELOGIO, 24);
  }

  @Test
  void selicEIpca_dadosReais_descartaDatasFuturas() {
    wm.stubFor(
        get(urlPathEqualTo("/dados/serie/bcdata.sgs.432/dados"))
            .willReturn(okJson(fixture("bcb/sgs-selic-meta-432.json"))));
    wm.stubFor(
        get(urlPathEqualTo("/dados/serie/bcdata.sgs.13522/dados"))
            .willReturn(okJson(fixture("bcb/sgs-ipca-12m-13522.json"))));

    ResultadoColeta<IndicadorColetado> r = cliente.buscar(BR);

    // A fixture da 432 so tem datas futuras (07 a 09/12/2026): todas descartadas.
    assertThat(r.itens()).filteredOn(i -> i.tipo().equals("JUROS")).isEmpty();
    assertThat(r.descartados()).isEqualTo(3);
    assertThat(r.itens())
        .filteredOn(i -> i.tipo().equals("INFLACAO"))
        .anySatisfy(i -> {
          assertThat(i.periodo()).isEqualTo(LocalDate.parse("2026-10-01"));
          assertThat(i.valor()).isEqualByComparingTo("4.63");
        });
    wm.verify(
        getRequestedFor(urlPathEqualTo("/dados/serie/bcdata.sgs.13522/dados"))
            .withQueryParam(
                "dataInicial",
                com.github.tomakehurst.wiremock.client.WireMock.equalTo("01/11/2024"))
            .withQueryParam(
                "dataFinal",
                com.github.tomakehurst.wiremock.client.WireMock.equalTo("22/11/2026")));
  }

  @Test
  void selic_guardaUltimoValorDeCadaMes() {
    String selic =
        "[{\"data\":\"03/11/2026\",\"valor\":\"14.25\"},{\"data\":\"21/11/2026\",\"valor\":\"13.75\"},{\"data\":\"31/10/2026\",\"valor\":\"14.25\"}]";
    wm.stubFor(get(urlPathEqualTo("/dados/serie/bcdata.sgs.432/dados")).willReturn(okJson(selic)));
    wm.stubFor(get(urlPathEqualTo("/dados/serie/bcdata.sgs.13522/dados")).willReturn(okJson("[]")));

    List<IndicadorColetado> juros = cliente.buscar(BR).itens();

    assertThat(juros)
        .extracting(IndicadorColetado::periodo, i -> i.valor().toPlainString())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(LocalDate.parse("2026-10-01"), "14.25"),
            org.assertj.core.groups.Tuple.tuple(LocalDate.parse("2026-11-01"), "13.75"));
  }

  @Test
  void serieInexistente404() {
    wm.stubFor(
        get(urlPathMatching("/dados/serie/.*"))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withBody(fixture("bcb/sgs-serie-inexistente-404.txt"))));
    assertThatThrownBy(() -> cliente.buscar(BR))
        .isInstanceOf(RespostaInvalidaException.class)
        .hasMessageContaining("nao existe");
  }

  @Test
  void respostaNaoModificada304SemCorpo_respostaInvalida() {
    wm.stubFor(get(urlPathMatching("/dados/serie/.*")).willReturn(aResponse().withStatus(304)));
    assertThatThrownBy(() -> cliente.buscar(BR)).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void formatoNaoLista() {
    wm.stubFor(get(urlPathMatching("/dados/serie/.*")).willReturn(okJson("{\"erro\":1}")));
    assertThatThrownBy(() -> cliente.buscar(BR)).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void pontoInvalidoEDescartado() {
    wm.stubFor(
        get(urlPathEqualTo("/dados/serie/bcdata.sgs.432/dados"))
            .willReturn(okJson("[{\"data\":\"xx\",\"valor\":\"1\"}]")));
    wm.stubFor(
        get(urlPathEqualTo("/dados/serie/bcdata.sgs.13522/dados"))
            .willReturn(okJson("[{\"data\":\"01/10/2026\",\"valor\":\"abc\"}]")));
    ResultadoColeta<IndicadorColetado> r = cliente.buscar(BR);
    assertThat(r.itens()).isEmpty();
    assertThat(r.descartados()).isEqualTo(2);
  }

  @Test
  void outrosPaises_naoChamaOSgs() {
    assertThat(cliente.buscar(List.of(new PaisConsulta("US", "USA"))).itens()).isEmpty();
    wm.verify(0, getRequestedFor(urlPathMatching("/dados/serie/.*")));
  }
}
