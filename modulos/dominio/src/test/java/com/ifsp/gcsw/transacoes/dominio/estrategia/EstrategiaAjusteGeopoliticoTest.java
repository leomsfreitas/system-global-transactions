package com.ifsp.gcsw.transacoes.dominio.estrategia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EstrategiaAjusteGeopoliticoTest {

  private final ProvedorFator provedor =
      Map.of("US", new BigDecimal("0.9820"), "BR", new BigDecimal("0.9850"))::get;

  @Test
  void paisDestino_padrao() {
    assertThat(TipoEstrategiaAjuste.PAIS_DESTINO.criar().fator("US", "BR", provedor))
        .isEqualTo(new BigDecimal("0.9850"));
  }

  @Test
  void paisOrigem() {
    assertThat(TipoEstrategiaAjuste.PAIS_ORIGEM.criar().fator("US", "BR", provedor))
        .isEqualTo(new BigDecimal("0.9820"));
  }

  @Test
  void menorFator_escolheOPaisDeMaiorRisco() {
    assertThat(TipoEstrategiaAjuste.MENOR_FATOR.criar().fator("US", "BR", provedor))
        .isEqualTo(new BigDecimal("0.9820"));
    assertThat(TipoEstrategiaAjuste.MENOR_FATOR.criar().fator("BR", "US", provedor))
        .isEqualTo(new BigDecimal("0.9820"));
  }
}
