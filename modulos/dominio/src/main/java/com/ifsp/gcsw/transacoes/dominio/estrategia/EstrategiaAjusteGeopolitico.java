package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.math.BigDecimal;

/** Define qual pais determina o fator geopolitico de uma conversao (secao 5.2, ADR 0006). */
public interface EstrategiaAjusteGeopolitico {

  BigDecimal fator(String paisOrigem, String paisDestino, ProvedorFator provedor);
}
