package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.worker.config.PropriedadesWorker;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.stereotype.Component;

/**
 * Roda as migrations Flyway com o usuario dono do schema (secao 6.2). Na AWS e a tarefa ECS unica
 * executada pelo CD antes do deploy; localmente e o servico "migracao" do docker compose.
 */
@Component
public class MigracaoJob implements Job {

  private final PropriedadesWorker.Migracao config;

  public MigracaoJob(PropriedadesWorker props) {
    this.config = props.migracao();
  }

  @Override
  public String nome() {
    return "MigracaoJob";
  }

  @Override
  public ResultadoJob executar() {
    if (config.url() == null || config.url().isBlank()) {
      throw new IllegalStateException("worker.migracao.url nao configurada (MIGRACAO_DB_URL)");
    }
    MigrateResult resultado =
        Flyway.configure()
            .dataSource(config.url(), config.usuario(), config.senha())
            .locations("classpath:db/migration")
            .load()
            .migrate();
    return new ResultadoJob(resultado.migrationsExecuted, resultado.migrationsExecuted, 0, 0, 0);
  }
}
