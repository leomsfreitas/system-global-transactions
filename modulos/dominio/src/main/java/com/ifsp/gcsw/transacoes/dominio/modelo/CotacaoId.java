package com.ifsp.gcsw.transacoes.dominio.modelo;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Chave composta de cotacao: a coluna de particao (data_hora) precisa fazer parte da PK (ADR 0012).
 */
public class CotacaoId implements Serializable {

  private Long id;
  private OffsetDateTime dataHora;

  public CotacaoId() {}

  public CotacaoId(Long id, OffsetDateTime dataHora) {
    this.id = id;
    this.dataHora = dataHora;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CotacaoId outro
        && Objects.equals(id, outro.id)
        && Objects.equals(dataHora, outro.dataHora);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, dataHora);
  }
}
