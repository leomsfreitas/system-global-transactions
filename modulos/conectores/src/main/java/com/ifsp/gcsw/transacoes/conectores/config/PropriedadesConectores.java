package com.ifsp.gcsw.transacoes.conectores.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** conectores.* (secao 6.5). Timeouts, retry e circuit breaker valem por fonte. */
@ConfigurationProperties("conectores")
public record PropriedadesConectores(
    @DefaultValue("3s") Duration timeoutConexao,
    @DefaultValue("15s") Duration timeoutLeitura,
    @DefaultValue("3") int tentativas,
    @DefaultValue("500ms") Duration esperaInicial,
    @DefaultValue("2.0") double multiplicadorEspera,
    @DefaultValue("10") int janelaCircuito,
    @DefaultValue("50") float percentualFalhasCircuito,
    @DefaultValue("60s") Duration esperaCircuitoAberto,
    @DefaultValue("24") int mesesHistoricoSgs,
    @DefaultValue("6") int anosHistoricoIndicadores,
    @DefaultValue("GOV_WGI_PV.EST") String indicadorEstabilidadePolitica,
    @DefaultValue Urls urls) {

  public record Urls(
      @DefaultValue("https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata") String bcbPtax,
      @DefaultValue("https://api.bcb.gov.br") String bcbSgs,
      @DefaultValue("https://data-api.ecb.europa.eu/service/data") String bce,
      @DefaultValue("https://api.imf.org/external/sdmx/3.0") String fmi,
      @DefaultValue("https://api.worldbank.org/v2") String bancoMundial) {}
}
