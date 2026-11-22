package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.conectores.bce.BceCliente;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.repositorio.MoedaRepositorio;
import com.ifsp.gcsw.transacoes.worker.config.PropriedadesWorker;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Taxas de referencia do BCE: uma vez por dia util, apos 17h de Frankfurt (secao 5.6). */
@Component
public class AtualizacaoCotacoesBceJob extends AtualizacaoCotacoes {

  public AtualizacaoCotacoesBceJob(
      BceCliente fonte,
      MoedaRepositorio moedas,
      GravacaoDados gravacao,
      CacheConsultas cache,
      Clock relogio,
      PropriedadesWorker props) {
    super(fonte, moedas, gravacao, cache, relogio, props.janelaCotacoesDias());
  }

  @Override
  public String nome() {
    return "AtualizacaoCotacoesBceJob";
  }
}
