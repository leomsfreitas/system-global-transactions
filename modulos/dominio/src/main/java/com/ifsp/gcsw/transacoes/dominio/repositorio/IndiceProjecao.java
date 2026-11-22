package com.ifsp.gcsw.transacoes.dominio.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IndiceProjecao {
  BigDecimal getIndiceRisco();

  BigDecimal getFatorAjuste();

  LocalDate getDataReferencia();
}
