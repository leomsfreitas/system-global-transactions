package com.ifsp.gcsw.transacoes.dominio.motor;

import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.ESCALA_FATOR;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.ESCALA_TAXA;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.MODO;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Motor de conversao (AT08). Classe pura: sem Spring e sem I/O.
 *
 * <p>valorConvertido = valor x taxa x fator, calculado em precisao exata e arredondado uma unica
 * vez no fim, com HALF_EVEN, para as casas decimais ISO 4217 da moeda de destino. Exemplo canonico:
 * 100,00 x 5,4210 x 0,9850 = 533,9685 que vira 533,97.
 */
public final class MotorConversao {

  public static final BigDecimal SEM_AJUSTE = BigDecimal.ONE.setScale(ESCALA_FATOR);

  public ResultadoConversao converter(
      BigDecimal valor, BigDecimal taxa, BigDecimal fator, int casasDecimaisDestino) {
    Objects.requireNonNull(valor, "valor");
    Objects.requireNonNull(taxa, "taxa");
    Objects.requireNonNull(fator, "fator");
    if (valor.signum() <= 0) {
      throw new IllegalArgumentException("O valor deve ser maior que zero");
    }
    if (taxa.signum() <= 0) {
      throw new IllegalArgumentException("A taxa de cambio deve ser maior que zero");
    }
    if (fator.signum() <= 0 || fator.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("O fator geopolitico deve estar no intervalo (0, 1]");
    }
    if (casasDecimaisDestino < 0) {
      throw new IllegalArgumentException("Casas decimais invalidas");
    }
    BigDecimal taxaArredondada = taxa.setScale(ESCALA_TAXA, MODO);
    BigDecimal fatorArredondado = fator.setScale(ESCALA_FATOR, MODO);
    BigDecimal convertido =
        valor
            .multiply(taxaArredondada)
            .multiply(fatorArredondado)
            .setScale(casasDecimaisDestino, MODO);
    return new ResultadoConversao(valor, taxaArredondada, fatorArredondado, convertido);
  }
}
