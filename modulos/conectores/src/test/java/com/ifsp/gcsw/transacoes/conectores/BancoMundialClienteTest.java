package com.ifsp.gcsw.transacoes.conectores;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.conectores.bancomundial.BancoMundialCliente;
import com.ifsp.gcsw.transacoes.conectores.bancomundial.RiscoPoliticoBancoMundial;
import com.ifsp.gcsw.transacoes.conectores.http.RespostaInvalidaException;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BancoMundialClienteTest extends BaseConectorTest {

  private static final List<PaisConsulta> PAISES =
      List.of(
          new PaisConsulta("BR", "BRA"),
          new PaisConsulta("US", "USA"),
          new PaisConsulta("GB", "GBR"),
          new PaisConsulta("JP", "JPN"),
          new PaisConsulta("CH", "CHE"),
          new PaisConsulta("CA", "CAN"),
          new PaisConsulta("AU", "AUS"),
          new PaisConsulta("CN", "CHN"),
          new PaisConsulta("MX", "MEX"),
          new PaisConsulta("AR", "ARG"),
          new PaisConsulta("DE", "DEU"),
          new PaisConsulta("FR", "FRA"),
          new PaisConsulta("IT", "ITA"),
          new PaisConsulta("ES", "ESP"),
          new PaisConsulta("NL", "NLD"),
          new PaisConsulta("EU", null));
  private BancoMundialCliente cliente;

  @BeforeEach
  void preparar() {
    cliente =
        new BancoMundialCliente(
            cliente("BM-" + System.nanoTime()), url(), "GOV_WGI_PV.EST", validador(), RELOGIO, 6);
  }

  @Test
  void inflacaoEPib_dadosReais() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/FP.CPI.TOTL.ZG"))
            .willReturn(okJson(fixture("bancomundial/inflacao-multi.json"))));
    wm.stubFor(
        get(urlPathMatching(".*/indicator/NY.GDP.MKTP.CD"))
            .willReturn(okJson(fixture("bancomundial/pib-multi.json"))));

    ResultadoColeta<IndicadorColetado> r = cliente.buscar(PAISES);

    assertThat(r.itens()).filteredOn(i -> i.tipo().equals("INFLACAO")).hasSize(15);
    assertThat(r.itens()).filteredOn(i -> i.tipo().equals("PIB")).hasSize(5);
    IndicadorColetado br =
        r.itens().stream()
            .filter(i -> i.pais().equals("BR") && i.tipo().equals("INFLACAO"))
            .findFirst()
            .orElseThrow();
    assertThat(br.valor()).isEqualByComparingTo("5.01675279604836");
    assertThat(br.periodo()).isEqualTo(LocalDate.parse("2025-01-01"));
    assertThat(br.fonte()).isEqualTo("BANCO_MUNDIAL");
  }

  @Test
  void estabilidadePolitica_codigoNovoGovWgi() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/GOV_WGI_PV.EST"))
            .willReturn(okJson(fixture("bancomundial/pv-est-multi.json"))));

    ResultadoColeta<IndicadorColetado> r = new RiscoPoliticoBancoMundial(cliente).buscar(PAISES);

    assertThat(r.itens()).hasSize(15).allMatch(i -> i.tipo().equals("ESTABILIDADE_POLITICA"));
    assertThat(r.itens())
        .filteredOn(i -> i.pais().equals("BR"))
        .singleElement()
        .satisfies(i -> assertThat(i.valor()).isEqualByComparingTo("-0.3845996"));
  }

  @Test
  void erroNoCorpoComHttp200_respostaInvalida() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/.*"))
            .willReturn(okJson(fixture("bancomundial/pais-invalido.json"))));
    assertThatThrownBy(() -> cliente.buscar(PAISES))
        .isInstanceOf(RespostaInvalidaException.class)
        .hasMessageContaining("recusou");
  }

  @Test
  void codigoAntigoPvEst_recusado() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/.*"))
            .willReturn(okJson(fixture("bancomundial/erro-parametro-invalido.json"))));
    assertThatThrownBy(() -> cliente.estabilidadePolitica(PAISES))
        .isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void vazio_semItens() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/.*"))
            .willReturn(okJson(fixture("bancomundial/vazio.json"))));
    assertThat(cliente.buscar(PAISES).itens()).isEmpty();
  }

  @Test
  void jsonMalformado() {
    wm.stubFor(
        get(urlPathMatching(".*/indicator/.*"))
            .willReturn(okJson(fixture("bancomundial/malformado.json"))));
    assertThatThrownBy(() -> cliente.buscar(PAISES)).isInstanceOf(RespostaInvalidaException.class);
  }

  @Test
  void paisNaoPedidoNaRespostaEDescartado() {
    String corpo =
        "[{\"page\":1},[{\"country\":{\"id\":\"ZZ\"},\"date\":\"2025\",\"value\":1.5},{\"country\":{\"id\":\"BR\"},\"date\":\"2025\",\"value\":null}]]";
    wm.stubFor(get(urlPathMatching(".*/indicator/.*")).willReturn(okJson(corpo)));
    ResultadoColeta<IndicadorColetado> r = cliente.estabilidadePolitica(PAISES);
    assertThat(r.itens()).isEmpty();
    assertThat(r.descartados()).isEqualTo(1);
  }

  @Test
  void semPaisesComIso_naoChama() {
    assertThat(cliente.buscar(List.of(new PaisConsulta("EU", null))).itens()).isEmpty();
  }
}
