package com.ifsp.gcsw.transacoes.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** worker.* */
@ConfigurationProperties("worker")
public record PropriedadesWorker(
    @DefaultValue("5") int janelaCotacoesDias,
    @DefaultValue("3") int mesesAntecedenciaParticoes,
    @DefaultValue("false") boolean executarAoIniciar,
    @DefaultValue Migracao migracao) {

  public record Migracao(String url, String usuario, String senha) {}
}
