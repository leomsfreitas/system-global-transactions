package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.math.BigDecimal;

/** Fornece o fator de ajuste vigente de um pais (codigo ISO alfa-2). */
@FunctionalInterface
public interface ProvedorFator {
  BigDecimal fatorAtual(String codigoPais);
}
