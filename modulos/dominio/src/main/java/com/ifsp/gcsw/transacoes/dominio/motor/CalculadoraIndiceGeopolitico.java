package com.ifsp.gcsw.transacoes.dominio.motor;

import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.DIVISAO;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.ESCALA_FATOR;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.ESCALA_INDICE;
import static com.ifsp.gcsw.transacoes.dominio.motor.Arredondamento.MODO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Formula do indice de risco e do fator de ajuste (secao 5.1, ADR 0005). Classe pura.
 *
 * <pre>
 * risco_politico  = clamp((max - PV) / (max - min) x 100, 0, 100)
 * risco_economico = clamp(inflacao / LIMITE_INFLACAO x 100, 0, 100)
 * indice_risco    = PESO_POLITICO x risco_politico + PESO_ECONOMICO x risco_economico  (2 casas)
 * fator_ajuste    = 1 - indice_risco / 100 x AJUSTE_MAXIMO                             (4 casas)
 * </pre>
 */
public final class CalculadoraIndiceGeopolitico {

  private static final BigDecimal CEM = BigDecimal.valueOf(100);

  private final ParametrosAjusteGeopolitico parametros;

  public CalculadoraIndiceGeopolitico(ParametrosAjusteGeopolitico parametros) {
    this.parametros = Objects.requireNonNull(parametros);
  }

  public BigDecimal riscoPolitico(BigDecimal estabilidadePolitica) {
    BigDecimal amplitude =
        parametros.estabilidadeMaxima().subtract(parametros.estabilidadeMinima());
    BigDecimal risco =
        parametros
            .estabilidadeMaxima()
            .subtract(estabilidadePolitica)
            .divide(amplitude, DIVISAO)
            .multiply(CEM);
    return limitar(risco);
  }

  public BigDecimal riscoEconomico(BigDecimal inflacaoPercentual) {
    BigDecimal risco =
        inflacaoPercentual.divide(parametros.limiteInflacao(), DIVISAO).multiply(CEM);
    return limitar(risco);
  }

  public BigDecimal indice(BigDecimal riscoPolitico, BigDecimal riscoEconomico) {
    return parametros
        .pesoPolitico()
        .multiply(riscoPolitico)
        .add(parametros.pesoEconomico().multiply(riscoEconomico))
        .setScale(ESCALA_INDICE, MODO);
  }

  public BigDecimal fator(BigDecimal indiceRisco) {
    BigDecimal reducao = indiceRisco.divide(CEM, DIVISAO).multiply(parametros.ajusteMaximo());
    return BigDecimal.ONE.subtract(reducao).setScale(ESCALA_FATOR, MODO);
  }

  public IndiceCalculado calcular(BigDecimal estabilidadePolitica, BigDecimal inflacaoPercentual) {
    BigDecimal politico = riscoPolitico(estabilidadePolitica);
    BigDecimal economico = riscoEconomico(inflacaoPercentual);
    BigDecimal indice = indice(politico, economico);
    return new IndiceCalculado(
        politico.setScale(ESCALA_INDICE, MODO),
        economico.setScale(ESCALA_INDICE, MODO),
        indice,
        fator(indice));
  }

  /**
   * Indice de um agregado (EU, secao 5.3): media ponderada pelo PIB; se algum peso faltar, media
   * simples dos disponiveis.
   */
  public BigDecimal mediaAgregada(List<ComponenteAgregado> componentes) {
    if (componentes == null || componentes.isEmpty()) {
      throw new IllegalArgumentException("Nenhum componente para o indice agregado");
    }
    boolean todosComPeso =
        componentes.stream().allMatch(c -> c.peso() != null && c.peso().signum() > 0);
    if (todosComPeso) {
      BigDecimal somaPesos = BigDecimal.ZERO;
      BigDecimal somaPonderada = BigDecimal.ZERO;
      for (ComponenteAgregado c : componentes) {
        somaPesos = somaPesos.add(c.peso());
        somaPonderada = somaPonderada.add(c.indiceRisco().multiply(c.peso()));
      }
      return somaPonderada.divide(somaPesos, DIVISAO).setScale(ESCALA_INDICE, MODO);
    }
    BigDecimal soma =
        componentes.stream()
            .map(ComponenteAgregado::indiceRisco)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return soma.divide(BigDecimal.valueOf(componentes.size()), DIVISAO)
        .setScale(ESCALA_INDICE, MODO);
  }

  private static BigDecimal limitar(BigDecimal valor) {
    if (valor.signum() < 0) {
      return BigDecimal.ZERO;
    }
    return valor.compareTo(CEM) > 0 ? CEM : valor;
  }

  /** Indice de um pais membro e seu peso (PIB em US$). */
  public record ComponenteAgregado(String pais, BigDecimal indiceRisco, BigDecimal peso) {}
}
