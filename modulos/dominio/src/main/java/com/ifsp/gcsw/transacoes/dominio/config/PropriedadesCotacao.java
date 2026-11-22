package com.ifsp.gcsw.transacoes.dominio.config;

import com.ifsp.gcsw.transacoes.dominio.taxa.Pivo;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** cotacao.* (secoes 5.4, 5.6 e 5.10). */
@ConfigurationProperties("cotacao")
public record PropriedadesCotacao(
    @DefaultValue("4d") Duration idadeMaxima,
    @DefaultValue("366") int historicoIntervaloMaximoDias,
    List<Pivo> pivos) {

  public PropriedadesCotacao {
    if (pivos == null || pivos.isEmpty()) {
      pivos = List.of(new Pivo("BRL", "BCB"), new Pivo("EUR", "BCE"));
    }
  }
}
