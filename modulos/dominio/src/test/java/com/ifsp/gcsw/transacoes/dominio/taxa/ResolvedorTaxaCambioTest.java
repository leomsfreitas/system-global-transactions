package com.ifsp.gcsw.transacoes.dominio.taxa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.dominio.excecao.DadoDesatualizadoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoIndisponivelException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolvedorTaxaCambioTest {

  private static final Instant AGORA = Instant.parse("2026-11-20T18:00:00Z");
  private static final OffsetDateTime HOJE_BCB = OffsetDateTime.parse("2026-11-20T16:04:18Z");
  private static final OffsetDateTime HOJE_BCE = OffsetDateTime.parse("2026-11-20T13:15:00Z");
  private static final OffsetDateTime ONTEM_BCE = OffsetDateTime.parse("2026-11-19T13:15:00Z");
  private static final List<Pivo> PIVOS = List.of(new Pivo("BRL", "BCB"), new Pivo("EUR", "BCE"));

  private final ConsultaCotacoesEmMemoria consulta = new ConsultaCotacoesEmMemoria();
  private final ResolvedorTaxaCambio resolvedor =
      new ResolvedorTaxaCambio(
          consulta, PIVOS, Duration.ofDays(4), Clock.fixed(AGORA, ZoneOffset.UTC));

  @Test
  void parDireto() {
    consulta.com("USD", "BRL", "5.4210", HOJE_BCB, "BCB");

    TaxaResolvida t = resolvedor.resolver("USD", "BRL");

    assertThat(t.valor()).isEqualTo(new BigDecimal("5.421000"));
    assertThat(t.metodo()).isEqualTo(MetodoTaxa.DIRETA);
    assertThat(t.fonte()).isEqualTo("BCB");
    assertThat(t.dataReferencia()).isEqualTo(HOJE_BCB);
  }

  @Test
  void parInverso_usaUmSobreValor() {
    consulta.com("USD", "BRL", "5.4210", HOJE_BCB, "BCB");

    TaxaResolvida t = resolvedor.resolver("BRL", "USD");

    // 1 / 5.4210 = 0.18446781...
    assertThat(t.valor()).isEqualTo(new BigDecimal("0.184468"));
    assertThat(t.metodo()).isEqualTo(MetodoTaxa.INVERSA);
    assertThat(t.fonte()).isEqualTo("BCB");
  }

  @Test
  void taxaCruzadaPeloBrl_comDadosDoBcb() {
    consulta
        .com("USD", "BRL", "5.88890", HOJE_BCB, "BCB")
        .com("GBP", "BRL", "7.40000", HOJE_BCB, "BCB");

    TaxaResolvida t = resolvedor.resolver("GBP", "USD");

    // 7.40000 / 5.88890 = 1.256601...
    assertThat(t.valor()).isEqualTo(new BigDecimal("1.256601"));
    assertThat(t.metodo()).isEqualTo(MetodoTaxa.CRUZADA);
    assertThat(t.fonte()).isEqualTo("Cruzada: BCB");
  }

  @Test
  void taxaCruzadaPeloEur_quandoBcbNaoCobre() {
    // CNY e MXN so existem no BCE (AT01)
    consulta
        .com("EUR", "CNY", "7.8000", HOJE_BCE, "BCE")
        .com("EUR", "MXN", "19.5000", HOJE_BCE, "BCE");

    TaxaResolvida t = resolvedor.resolver("CNY", "MXN");

    // (1/7.8) / (1/19.5) = 19.5 / 7.8 = 2.5
    assertThat(t.valor()).isEqualTo(new BigDecimal("2.500000"));
    assertThat(t.fonte()).isEqualTo("Cruzada: BCE");
  }

  @Test
  void taxaCruzadaMista_pernaDeIdaEPernaDeVolta() {
    // BRL->CNY: BCE tem EUR->BRL e EUR->CNY; BCB nao tem CNY.
    consulta
        .com("EUR", "BRL", "6.4672", HOJE_BCE, "BCE")
        .com("EUR", "CNY", "7.8000", HOJE_BCE, "BCE");

    TaxaResolvida t = resolvedor.resolver("BRL", "CNY");

    // 7.8 / 6.4672 = 1.206086...
    assertThat(t.valor()).isEqualTo(new BigDecimal("1.206086"));
    assertThat(t.fonte()).isEqualTo("Cruzada: BCE");
  }

  @Test
  void taxaCruzada_exigeMesmaData() {
    // As pernas mais recentes estao em dias diferentes, mas ha um dia em comum (ontem).
    consulta
        .com("EUR", "CNY", "7.8000", HOJE_BCE, "BCE")
        .com("EUR", "CNY", "7.7000", ONTEM_BCE, "BCE")
        .com("EUR", "MXN", "19.2500", ONTEM_BCE, "BCE");

    TaxaResolvida t = resolvedor.resolver("CNY", "MXN");

    assertThat(t.valor()).isEqualTo(new BigDecimal("2.500000"));
    assertThat(t.dataReferencia()).isEqualTo(ONTEM_BCE);
  }

  @Test
  void taxaCruzada_semDiaEmComum_indisponivel() {
    consulta
        .com("EUR", "CNY", "7.8000", HOJE_BCE, "BCE")
        .com("EUR", "MXN", "19.2500", ONTEM_BCE, "BCE");

    assertThatThrownBy(() -> resolvedor.resolver("CNY", "MXN"))
        .isInstanceOf(DadoIndisponivelException.class)
        .isNotInstanceOf(DadoDesatualizadoException.class);
  }

  @Test
  void mesmaMoeda_taxaUm() {
    TaxaResolvida t = resolvedor.resolver("USD", "USD");
    assertThat(t.valor()).isEqualTo(new BigDecimal("1.000000"));
    assertThat(t.metodo()).isEqualTo(MetodoTaxa.IDENTIDADE);
    resolvedor.exigirFrescor(t);
  }

  @Test
  void parSemNenhumaFonte_indisponivel() {
    assertThatThrownBy(() -> resolvedor.resolver("USD", "BRL"))
        .isInstanceOf(DadoIndisponivelException.class)
        .hasMessageContaining("USD/BRL");
  }

  @Test
  void cotacaoMaisAntigaQueIdadeMaxima_desatualizada() {
    OffsetDateTime antiga = OffsetDateTime.parse("2026-11-15T16:00:00Z");
    consulta.com("USD", "BRL", "5.4210", antiga, "BCB");

    assertThatThrownBy(() -> resolvedor.resolver("USD", "BRL"))
        .isInstanceOfSatisfying(
            DadoDesatualizadoException.class,
            e -> assertThat(e.getDataReferencia()).isEqualTo(antiga))
        .hasMessageContaining("2026-11-15");
  }

  @Test
  void diretaDesatualizada_masCruzadaFresca_usaCruzada() {
    consulta
        .com("EUR", "USD", "1.1000", OffsetDateTime.parse("2026-11-01T13:15:00Z"), "BCE")
        .com("EUR", "BRL", "6.4672", HOJE_BCB, "BCB")
        .com("USD", "BRL", "5.8889", HOJE_BCB, "BCB");

    TaxaResolvida t = resolvedor.resolver("EUR", "USD");

    assertThat(t.metodo()).isEqualTo(MetodoTaxa.CRUZADA);
    assertThat(t.valor()).isEqualTo(new BigDecimal("1.098202"));
  }

  @Test
  void limiteDeFrescor_exatamenteQuatroDias_aindaValido() {
    consulta.com(
        "USD",
        "BRL",
        "5.4210",
        OffsetDateTime.ofInstant(AGORA.minus(Duration.ofDays(4)), ZoneOffset.UTC),
        "BCB");
    assertThat(resolvedor.resolver("USD", "BRL").valor()).isEqualByComparingTo("5.421");
  }

  @Test
  void historico_direto_umaPorDia() {
    consulta
        .com("USD", "BRL", "5.80", OffsetDateTime.parse("2026-11-18T13:00:00Z"), "BCB")
        .com("USD", "BRL", "5.81", OffsetDateTime.parse("2026-11-18T16:00:00Z"), "BCB")
        .com("USD", "BRL", "5.89", OffsetDateTime.parse("2026-11-19T16:00:00Z"), "BCB");

    List<TaxaResolvida> h =
        resolvedor.historico(
            "USD", "BRL", LocalDate.parse("2026-11-18"), LocalDate.parse("2026-11-19"));

    assertThat(h)
        .extracting(TaxaResolvida::valor)
        .containsExactly(new BigDecimal("5.810000"), new BigDecimal("5.890000"));
  }

  @Test
  void historico_inverso() {
    consulta.com("USD", "BRL", "5.00", OffsetDateTime.parse("2026-11-18T16:00:00Z"), "BCB");

    List<TaxaResolvida> h =
        resolvedor.historico(
            "BRL", "USD", LocalDate.parse("2026-11-18"), LocalDate.parse("2026-11-18"));

    assertThat(h)
        .singleElement()
        .satisfies(t -> {
          assertThat(t.valor()).isEqualTo(new BigDecimal("0.200000"));
          assertThat(t.metodo()).isEqualTo(MetodoTaxa.INVERSA);
        });
  }

  @Test
  void historico_cruzado_soDiasComAsDuasPernas() {
    consulta
        .com("EUR", "CNY", "7.80", OffsetDateTime.parse("2026-11-18T13:15:00Z"), "BCE")
        .com("EUR", "CNY", "7.70", OffsetDateTime.parse("2026-11-19T13:15:00Z"), "BCE")
        .com("EUR", "MXN", "19.50", OffsetDateTime.parse("2026-11-18T13:15:00Z"), "BCE");

    List<TaxaResolvida> h =
        resolvedor.historico(
            "CNY", "MXN", LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));

    assertThat(h)
        .singleElement()
        .satisfies(t -> {
          assertThat(t.valor()).isEqualTo(new BigDecimal("2.500000"));
          assertThat(t.fonte()).isEqualTo("Cruzada: BCE");
        });
  }

  @Test
  void historico_semDados_listaVazia() {
    assertThat(
            resolvedor.historico(
                "USD", "BRL", LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02")))
        .isEmpty();
  }
}
