package com.ifsp.gcsw.transacoes.dominio.repositorio;

import java.math.BigDecimal;
import java.time.Instant;

public interface CotacaoProjecao {
  BigDecimal getValor();

  Instant getDataHora();

  String getFonte();

  Long getFonteId();
}
