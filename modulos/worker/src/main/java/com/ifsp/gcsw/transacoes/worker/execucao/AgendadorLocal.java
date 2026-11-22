package com.ifsp.gcsw.transacoes.worker.execucao;

import com.ifsp.gcsw.transacoes.worker.config.PropriedadesWorker;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendamento local (perfil local), espelhando os schedules do EventBridge (secao 5.6). Na AWS quem
 * dispara e o EventBridge Scheduler (infra/modulos/eventbridge).
 */
@Component
@Profile("local")
@EnableScheduling
public class AgendadorLocal {

  private final ExecutorJobs executor;
  private final PropriedadesWorker props;

  public AgendadorLocal(ExecutorJobs executor, PropriedadesWorker props) {
    this.executor = executor;
    this.props = props;
  }

  /** Ao subir com make up, roda tudo uma vez para o ambiente ja ter dados frescos do WireMock. */
  @EventListener(ApplicationReadyEvent.class)
  public void aoIniciar() {
    if (props.executarAoIniciar()) {
      executor.executar("ManutencaoParticoesJob");
      executor.executar("AtualizacaoCotacoesBcbJob");
      executor.executar("AtualizacaoCotacoesBceJob");
      executor.executar("AtualizacaoIndicadoresJob");
      executor.executar("CalculoIndiceGeopoliticoJob");
    }
  }

  @Scheduled(cron = "${worker.agenda.bcb}", zone = "America/Sao_Paulo")
  public void cotacoesBcb() {
    executor.executar("AtualizacaoCotacoesBcbJob");
  }

  @Scheduled(cron = "${worker.agenda.bce}", zone = "Europe/Berlin")
  public void cotacoesBce() {
    executor.executar("AtualizacaoCotacoesBceJob");
  }

  @Scheduled(cron = "${worker.agenda.indicadores}", zone = "UTC")
  public void indicadores() {
    executor.executar("AtualizacaoIndicadoresJob");
  }

  @Scheduled(cron = "${worker.agenda.indice}", zone = "UTC")
  public void indice() {
    executor.executar("CalculoIndiceGeopoliticoJob");
  }

  @Scheduled(cron = "${worker.agenda.particoes}", zone = "UTC")
  public void particoes() {
    executor.executar("ManutencaoParticoesJob");
  }
}
