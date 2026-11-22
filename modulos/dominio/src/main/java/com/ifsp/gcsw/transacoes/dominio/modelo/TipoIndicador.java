package com.ifsp.gcsw.transacoes.dominio.modelo;

/**
 * Tipos de indicador economico. ESTABILIDADE_POLITICA e interno (ADR 0005), nao e exposto na API.
 */
public enum TipoIndicador {
  INFLACAO,
  JUROS,
  PIB,
  ESTABILIDADE_POLITICA;

  public boolean publico() {
    return this != ESTABILIDADE_POLITICA;
  }
}
