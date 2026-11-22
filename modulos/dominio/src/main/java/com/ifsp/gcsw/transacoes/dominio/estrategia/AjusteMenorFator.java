package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.math.BigDecimal;

/** Vale o menor fator entre origem e destino (o pais de maior risco), o mais conservador. */
public final class AjusteMenorFator implements EstrategiaAjusteGeopolitico {

  @Override
  public BigDecimal fator(String paisOrigem, String paisDestino, ProvedorFator provedor) {
    BigDecimal origem = provedor.fatorAtual(paisOrigem);
    BigDecimal destino = provedor.fatorAtual(paisDestino);
    return origem.min(destino);
  }
}
