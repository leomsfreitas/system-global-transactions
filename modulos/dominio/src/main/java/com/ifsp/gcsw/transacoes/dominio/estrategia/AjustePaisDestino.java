package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.math.BigDecimal;

/** Padrao: vale o pais da moeda de destino. */
public final class AjustePaisDestino implements EstrategiaAjusteGeopolitico {

  @Override
  public BigDecimal fator(String paisOrigem, String paisDestino, ProvedorFator provedor) {
    return provedor.fatorAtual(paisDestino);
  }
}
