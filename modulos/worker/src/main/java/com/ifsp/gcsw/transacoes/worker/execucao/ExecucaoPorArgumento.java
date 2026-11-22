package com.ifsp.gcsw.transacoes.worker.execucao;

import com.ifsp.gcsw.transacoes.worker.job.ResultadoJob;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Modo tarefa ECS: com --job=NOME roda o job e encerra o processo (codigo 0 em sucesso, 1 em
 * falha). Sem --job, nao faz nada (perfil local usa o agendador).
 */
@Component
public class ExecucaoPorArgumento implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ExecucaoPorArgumento.class);

  private final ExecutorJobs executor;
  private final ApplicationContext contexto;
  private final boolean encerrar;

  public ExecucaoPorArgumento(
      ExecutorJobs executor,
      ApplicationContext contexto,
      @org.springframework.beans.factory.annotation.Value("${worker.encerrar-apos-job:true}")
          boolean encerrar) {
    this.executor = executor;
    this.contexto = contexto;
    this.encerrar = encerrar;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<String> nomes = args.getOptionValues("job");
    if (nomes == null || nomes.isEmpty()) {
      return;
    }
    boolean falhou = false;
    for (String nome : nomes) {
      ResultadoJob r = executor.executar(nome);
      falhou |= r.falhas() > 0;
    }
    int codigo = falhou ? 1 : 0;
    LOG.info("Execucao por argumento encerrada com codigo {}", codigo);
    if (encerrar) {
      ExitCodeGenerator gerador = () -> codigo;
      System.exit(SpringApplication.exit(contexto, gerador));
    }
  }
}
