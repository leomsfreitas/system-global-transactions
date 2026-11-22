package com.ifsp.gcsw.transacoes.dominio.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.dominio.motor.CalculadoraIndiceGeopolitico.ComponenteAgregado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculadoraIndiceGeopoliticoTest {

  private final CalculadoraIndiceGeopolitico calc =
      new CalculadoraIndiceGeopolitico(ParametrosAjusteGeopolitico.PADRAO);

  @Test
  void riscoTrintaGeraFator0_9850_comoNoDocumento() {
    assertThat(calc.fator(new BigDecimal("30.00"))).isEqualTo(new BigDecimal("0.9850"));
  }

  @ParameterizedTest
  @CsvSource({"2.5, 0", "-2.5, 100", "0, 50", "3.1, 0", "-3, 100", "-0.3845996, 57.69199200"})
  void riscoPolitico_normalizaEscalaDoWgi(String pv, String esperado) {
    assertThat(calc.riscoPolitico(new BigDecimal(pv))).isEqualByComparingTo(esperado);
  }

  @ParameterizedTest
  @CsvSource({"0, 0", "25, 50", "50, 100", "219.88, 100", "-1.2, 0"})
  void riscoEconomico_limitadoEntreZeroECem(String inflacao, String esperado) {
    assertThat(calc.riscoEconomico(new BigDecimal(inflacao))).isEqualByComparingTo(esperado);
  }

  @Test
  void calculo_dadosReaisDoBrasil2025() {
    // GOV_WGI_PV.EST = -0.3845996 e inflacao 5.01675279604836 (fixtures do Banco Mundial)
    IndiceCalculado r =
        calc.calcular(new BigDecimal("-0.3845996"), new BigDecimal("5.01675279604836"));

    assertThat(r.riscoPolitico()).isEqualByComparingTo("57.69");
    assertThat(r.riscoEconomico()).isEqualByComparingTo("10.03");
    assertThat(r.indiceRisco()).isEqualTo(new BigDecimal("38.63"));
    assertThat(r.fatorAjuste()).isEqualTo(new BigDecimal("0.9807"));
  }

  @Test
  void fatorNuncaFicaAbaixoDeUmMenosAjusteMaximo() {
    assertThat(calc.fator(new BigDecimal("100"))).isEqualTo(new BigDecimal("0.9500"));
    assertThat(calc.fator(BigDecimal.ZERO)).isEqualTo(new BigDecimal("1.0000"));
  }

  @Test
  void mediaAgregada_ponderadaPeloPib() {
    BigDecimal media =
        calc.mediaAgregada(
            List.of(
                new ComponenteAgregado("DE", new BigDecimal("30"), new BigDecimal("3")),
                new ComponenteAgregado("FR", new BigDecimal("20"), new BigDecimal("1"))));
    assertThat(media).isEqualTo(new BigDecimal("27.50"));
  }

  @Test
  void mediaAgregada_semPesoEmAlgumPais_usaMediaSimples() {
    BigDecimal media =
        calc.mediaAgregada(
            List.of(
                new ComponenteAgregado("DE", new BigDecimal("30"), new BigDecimal("3")),
                new ComponenteAgregado("FR", new BigDecimal("20"), null)));
    assertThat(media).isEqualTo(new BigDecimal("25.00"));
  }

  @Test
  void mediaAgregada_semComponentes_falha() {
    assertThatThrownBy(() -> calc.mediaAgregada(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parametrosInvalidosSaoRejeitados() {
    BigDecimal um = BigDecimal.ONE;
    BigDecimal meio = new BigDecimal("0.5");
    assertThatThrownBy(() ->
            new ParametrosAjusteGeopolitico(
                meio,
                meio.add(meio),
                um,
                new BigDecimal("0.05"),
                new BigDecimal("-2.5"),
                new BigDecimal("2.5")))
        .hasMessageContaining("soma");
    assertThatThrownBy(() ->
            new ParametrosAjusteGeopolitico(
                new BigDecimal("-0.1"),
                new BigDecimal("1.1"),
                um,
                new BigDecimal("0.05"),
                new BigDecimal("-2.5"),
                new BigDecimal("2.5")))
        .hasMessageContaining("negativos");
    assertThatThrownBy(() ->
            new ParametrosAjusteGeopolitico(
                meio,
                meio,
                BigDecimal.ZERO,
                new BigDecimal("0.05"),
                new BigDecimal("-2.5"),
                new BigDecimal("2.5")))
        .hasMessageContaining("inflacao");
    assertThatThrownBy(() ->
            new ParametrosAjusteGeopolitico(
                meio, meio, um, um, new BigDecimal("-2.5"), new BigDecimal("2.5")))
        .hasMessageContaining("ajuste");
    assertThatThrownBy(() ->
            new ParametrosAjusteGeopolitico(
                meio,
                meio,
                um,
                new BigDecimal("0.05"),
                new BigDecimal("2.5"),
                new BigDecimal("2.5")))
        .hasMessageContaining("estabilidade");
  }
}
