package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.fmi.FmiCliente;
import com.ifsp.gcsw.transacoes.conectores.fmi.FmiClienteTestAcesso;
import com.ifsp.gcsw.transacoes.conectores.http.FonteIndisponivelException;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FmiClienteTest extends BaseConectorTest {

  private static final List<PaisConsulta> PAISES =
      List.of(
          new PaisConsulta("AR", "ARG"),
          new PaisConsulta("BR", "BRA"),
          new PaisConsulta("MX", "MEX"),
          new PaisConsulta("CN", "CHN"));
  private FmiCliente cliente;

  @BeforeEach
  void preparar() {
    cliente = new FmiCliente(cliente("FMI-" + System.nanoTime()), url(), validador(), RELOGIO, 6);
  }

  @Test
  void inflacaoAnualEJurosMensais_dadosReais() {
    wm.stubFor(
        get(urlPathMatching(".*/IMF.STA/CPI/.*"))
            .willReturn(okJson(fixture("fmi/cpi-inflacao-anual-ar-br.json"))));
    wm.stubFor(
        get(urlPathMatching(".*/IMF.STA/MFS_IR/.*"))
            .willReturn(okJson(fixture("fmi/mfs-ir-juros-politica-mex-chn.json"))));

    ResultadoColeta<IndicadorColetado> r = cliente.buscar(PAISES);

    // Janela de 6 anos a partir de 2026: 2020 em diante
    List<IndicadorColetado> inflacaoAr =
        r.itens().stream()
            .filter(i -> i.pais().equals("AR") && i.tipo().equals("INFLACAO"))
            .toList();
    assertThat(inflacaoAr).allMatch(i -> i.periodo().getYear() >= 2020);
    assertThat(inflacaoAr)
        .anySatisfy(i -> {
          assertThat(i.periodo()).isEqualTo(LocalDate.parse("2024-01-01"));
          assertThat(i.valor()).isEqualByComparingTo("219.883929014578");
        });
    assertThat(r.itens())
        .filteredOn(i -> i.tipo().equals("JUROS"))
        .extracting(IndicadorColetado::periodo)
        .containsExactlyInAnyOrder(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-04-01"),
            LocalDate.parse("2026-05-01"));
    assertThat(r.itens())
        .filteredOn(i -> i.tipo().equals("JUROS"))
        .allMatch(i -> i.pais().equals("MX"));
  }

  @Test
  void semSeries_coletaVazia() {
    wm.stubFor(
        get(urlPathMatching(".*/IMF.STA/.*")).willReturn(okJson(fixture("fmi/cpi-vazio.json"))));
    assertThat(cliente.buscar(PAISES).itens()).isEmpty();
  }

  @Test
  void naoEncontrado404_coletaVazia() {
    wm.stubFor(get(urlPathMatching(".*/IMF.STA/.*")).willReturn(aResponse().withStatus(404)));
    assertThat(cliente.buscar(PAISES).itens()).isEmpty();
  }

  @Test
  void jsonMalformado() {
    wm.stubFor(
        get(urlPathMatching(".*/IMF.STA/.*")).willReturn(okJson(fixture("fmi/malformado.json"))));
    assertThatThrownBy(() -> cliente.buscar(PAISES)).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void formatoInesperado() {
    wm.stubFor(get(urlPathMatching(".*/IMF.STA/.*")).willReturn(okJson("{\"outro\":1}")));
    assertThatThrownBy(() -> cliente.buscar(PAISES)).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void erro502_indisponivel() {
    wm.stubFor(get(urlPathMatching(".*/IMF.STA/.*")).willReturn(aResponse().withStatus(502)));
    assertThatThrownBy(() -> cliente.buscar(PAISES)).isInstanceOf(FonteIndisponivelException.class);
  }

  @Test
  void interpretaPeriodos() {
    assertThat(FmiClienteTestAcesso.periodo("2025")).isEqualTo(LocalDate.parse("2025-01-01"));
    assertThat(FmiClienteTestAcesso.periodo("2026-M05")).isEqualTo(LocalDate.parse("2026-05-01"));
    assertThat(FmiClienteTestAcesso.periodo("2026-M13")).isNull();
    assertThat(FmiClienteTestAcesso.periodo("2026-Q1")).isNull();
  }
}
