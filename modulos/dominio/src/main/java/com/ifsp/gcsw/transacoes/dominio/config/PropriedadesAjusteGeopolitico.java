package com.ifsp.gcsw.transacoes.dominio.config;

import com.ifsp.gcsw.transacoes.dominio.estrategia.TipoEstrategiaAjuste;
import com.ifsp.gcsw.transacoes.dominio.motor.ParametrosAjusteGeopolitico;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** ajuste-geopolitico.* (secoes 5.1, 5.2 e 5.3; ADRs 0005, 0006 e 0008). */
@ConfigurationProperties("ajuste-geopolitico")
public record PropriedadesAjusteGeopolitico(
    @DefaultValue("PAIS_DESTINO") TipoEstrategiaAjuste estrategia,
    @DefaultValue("0.6") BigDecimal pesoPolitico,
    @DefaultValue("0.4") BigDecimal pesoEconomico,
    @DefaultValue("50") BigDecimal limiteInflacao,
    @DefaultValue("0.05") BigDecimal ajusteMaximo,
    @DefaultValue("-2.5") BigDecimal estabilidadeMinima,
    @DefaultValue("2.5") BigDecimal estabilidadeMaxima,
    Map<String, List<String>> agregados) {

  public PropriedadesAjusteGeopolitico {
    if (agregados == null || agregados.isEmpty()) {
      agregados = Map.of("EU", List.of("DE", "FR", "IT", "ES", "NL"));
    }
  }

  public ParametrosAjusteGeopolitico parametros() {
    return new ParametrosAjusteGeopolitico(
        pesoPolitico,
        pesoEconomico,
        limiteInflacao,
        ajusteMaximo,
        estabilidadeMinima,
        estabilidadeMaxima);
  }
}
