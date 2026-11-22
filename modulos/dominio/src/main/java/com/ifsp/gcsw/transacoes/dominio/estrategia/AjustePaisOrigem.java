package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.math.BigDecimal;

/** Vale o pais da moeda de origem. */
public final class AjustePaisOrigem implements EstrategiaAjusteGeopolitico {

  @Override
  public BigDecimal fator(String paisOrigem, String paisDestino, ProvedorFator provedor) {
    return provedor.fatorAtual(paisOrigem);
  }
}
