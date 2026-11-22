package com.ifsp.gcsw.transacoes.dominio.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IndicadorProjecao {
  String getTipo();

  BigDecimal getValor();

  LocalDate getPeriodo();

  String getFonte();
}
