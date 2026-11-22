package com.ifsp.gcsw.transacoes.dominio.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MotorConversaoTest {

  private final MotorConversao motor = new MotorConversao();

  @Test
  void exemploCanonico_100UsdParaBrl_resultaEm533_97() {
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("100.00"), new BigDecimal("5.4210"), new BigDecimal("0.9850"), 2);

    assertThat(r.valorConvertido()).isEqualByComparingTo("533.97");
    assertThat(r.valorConvertido().scale()).isEqualTo(2);
    assertThat(r.taxaCambio()).isEqualTo(new BigDecimal("5.421000"));
    assertThat(r.fatorGeopolitico()).isEqualTo(new BigDecimal("0.9850"));
    assertThat(r.valorOriginal()).isEqualTo(new BigDecimal("100.00"));
  }

  @Test
  void ajusteDesligado_usaFatorUm() {
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("100.00"), new BigDecimal("5.4210"), MotorConversao.SEM_AJUSTE, 2);

    assertThat(r.valorConvertido()).isEqualByComparingTo("542.10");
    assertThat(r.fatorGeopolitico()).isEqualTo(new BigDecimal("1.0000"));
  }

  @Test
  void moedaComZeroCasasDecimais_arredondaParaInteiro() {
    // 100 USD -> JPY a 155.123456 com fator 0.9913 = 15377.388... -> 15377
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("100"), new BigDecimal("155.123456"), new BigDecimal("0.9913"), 0);

    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal("15377"));
  }

  @ParameterizedTest
  @CsvSource({
    // valor, taxa, fator, casas, esperado  (HALF_EVEN no arredondamento final)
    "1.00, 1.000000, 0.9875, 2, 0.99",
    "1.00, 1.000000, 0.9825, 2, 0.98",
    "10.00, 0.500000, 1.0000, 2, 5.00",
    "0.01, 5.421000, 0.9850, 2, 0.05",
    "0.01, 0.036850, 1.0000, 2, 0.00",
    "2.5, 1.000000, 1.0000, 0, 2",
    "3.5, 1.000000, 1.0000, 0, 4"
  })
  void arredondaComHalfEven(String valor, String taxa, String fator, int casas, String esperado) {
    ResultadoConversao r =
        motor.converter(new BigDecimal(valor), new BigDecimal(taxa), new BigDecimal(fator), casas);
    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal(esperado));
  }

  @Test
  void valoresMuitoGrandes_naoPerdemPrecisao() {
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("1000000000000"),
            new BigDecimal("155.123456"),
            new BigDecimal("0.9850"),
            0);

    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal("152796604160000"));
  }

  @Test
  void valoresMuitoPequenos_arredondamParaZeroSemErro() {
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("0.000001"), new BigDecimal("0.006443"), new BigDecimal("0.9800"), 2);

    assertThat(r.valorConvertido()).isEqualByComparingTo("0.00");
  }

  @Test
  void taxaEFatorSaoArredondadosParaSeisEQuatroCasas() {
    ResultadoConversao r =
        motor.converter(
            new BigDecimal("1"), new BigDecimal("5.42104999"), new BigDecimal("0.98505"), 4);

    assertThat(r.taxaCambio()).isEqualTo(new BigDecimal("5.421050"));
    assertThat(r.fatorGeopolitico()).isEqualTo(new BigDecimal("0.9850"));
  }

  @Test
  void propriedade_resultadoComAjusteNuncaSuperaSemAjuste() {
    Random aleatorio = new Random(42);
    for (int i = 0; i < 5_000; i++) {
      BigDecimal valor = BigDecimal.valueOf(1 + aleatorio.nextInt(1_000_000), 2);
      BigDecimal taxa = BigDecimal.valueOf(1 + aleatorio.nextInt(200_000_000), 6);
      BigDecimal fator = BigDecimal.valueOf(1 + aleatorio.nextInt(10_000), 4);
      int casas = aleatorio.nextBoolean() ? 2 : 0;

      BigDecimal comAjuste = motor.converter(valor, taxa, fator, casas).valorConvertido();
      BigDecimal semAjuste =
          motor.converter(valor, taxa, MotorConversao.SEM_AJUSTE, casas).valorConvertido();

      assertThat(comAjuste).isLessThanOrEqualTo(semAjuste);
    }
  }

  @Test
  void rejeitaEntradasInvalidas() {
    BigDecimal um = BigDecimal.ONE;
    assertThatThrownBy(() -> motor.converter(BigDecimal.ZERO, um, um, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> motor.converter(um, new BigDecimal("-1"), um, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> motor.converter(um, um, new BigDecimal("1.0001"), 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> motor.converter(um, um, BigDecimal.ZERO, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> motor.converter(um, um, um, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> motor.converter(null, um, um, 2))
        .isInstanceOf(NullPointerException.class);
  }
}
