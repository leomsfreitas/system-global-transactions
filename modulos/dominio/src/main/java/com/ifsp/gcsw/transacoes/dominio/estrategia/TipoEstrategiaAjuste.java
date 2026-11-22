package com.ifsp.gcsw.transacoes.dominio.estrategia;

import java.util.function.Supplier;

/** Selecao por configuracao: ajuste-geopolitico.estrategia. */
public enum TipoEstrategiaAjuste {
  PAIS_DESTINO(AjustePaisDestino::new),
  PAIS_ORIGEM(AjustePaisOrigem::new),
  MENOR_FATOR(AjusteMenorFator::new);

  private final Supplier<EstrategiaAjusteGeopolitico> fabrica;

  TipoEstrategiaAjuste(Supplier<EstrategiaAjusteGeopolitico> fabrica) {
    this.fabrica = fabrica;
  }

  public EstrategiaAjusteGeopolitico criar() {
    return fabrica.get();
  }
}
