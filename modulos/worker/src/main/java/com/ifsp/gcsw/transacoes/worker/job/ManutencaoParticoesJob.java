package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.worker.config.PropriedadesWorker;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Cria com antecedencia as particoes mensais de cotacao (secao 5.9). Idempotente. */
@Component
public class ManutencaoParticoesJob implements Job {

  private final GravacaoDados gravacao;
  private final Clock relogio;
  private final int mesesAntecedencia;

  public ManutencaoParticoesJob(GravacaoDados gravacao, Clock relogio, PropriedadesWorker props) {
    this.gravacao = gravacao;
    this.relogio = relogio;
    this.mesesAntecedencia = props.mesesAntecedenciaParticoes();
  }

  @Override
  public String nome() {
    return "ManutencaoParticoesJob";
  }

  @Override
  public ResultadoJob executar() {
    LocalDate mes = LocalDate.now(relogio).withDayOfMonth(1);
    for (int i = 0; i <= mesesAntecedencia; i++) {
      gravacao.criarParticaoCotacao(mes.plusMonths(i));
    }
    return new ResultadoJob(mesesAntecedencia + 1, 0, mesesAntecedencia + 1, 0, 0);
  }
}
