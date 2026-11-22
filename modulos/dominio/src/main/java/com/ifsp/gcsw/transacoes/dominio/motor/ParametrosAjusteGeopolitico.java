package com.ifsp.gcsw.transacoes.dominio.motor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pesos e limites da formula do indice (secao 5.1, ADR 0005). Vem de ajuste-geopolitico.* no
 * application.yml.
 */
public record ParametrosAjusteGeopolitico(
    BigDecimal pesoPolitico,
    BigDecimal pesoEconomico,
    BigDecimal limiteInflacao,
    BigDecimal ajusteMaximo,
    BigDecimal estabilidadeMinima,
    BigDecimal estabilidadeMaxima) {

  public static final ParametrosAjusteGeopolitico PADRAO =
      new ParametrosAjusteGeopolitico(
          new BigDecimal("0.6"),
          new BigDecimal("0.4"),
          new BigDecimal("50"),
          new BigDecimal("0.05"),
          new BigDecimal("-2.5"),
          new BigDecimal("2.5"));

  public ParametrosAjusteGeopolitico {
    Objects.requireNonNull(pesoPolitico);
    Objects.requireNonNull(pesoEconomico);
    Objects.requireNonNull(limiteInflacao);
    Objects.requireNonNull(ajusteMaximo);
    Objects.requireNonNull(estabilidadeMinima);
    Objects.requireNonNull(estabilidadeMaxima);
    if (pesoPolitico.signum() < 0 || pesoEconomico.signum() < 0) {
      throw new IllegalArgumentException("Pesos nao podem ser negativos");
    }
    if (pesoPolitico.add(pesoEconomico).compareTo(BigDecimal.ONE) != 0) {
      throw new IllegalArgumentException("A soma dos pesos deve ser 1");
    }
    if (limiteInflacao.signum() <= 0) {
      throw new IllegalArgumentException("O limite de inflacao deve ser positivo");
    }
    if (ajusteMaximo.signum() < 0 || ajusteMaximo.compareTo(BigDecimal.ONE) >= 0) {
      throw new IllegalArgumentException("O ajuste maximo deve estar em [0, 1)");
    }
    if (estabilidadeMaxima.compareTo(estabilidadeMinima) <= 0) {
      throw new IllegalArgumentException("Escala de estabilidade invalida");
    }
  }
}
