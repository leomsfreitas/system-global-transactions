package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.conectores.bcb.BcbPtaxCliente;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.repositorio.MoedaRepositorio;
import com.ifsp.gcsw.transacoes.worker.config.PropriedadesWorker;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** PTAX do BCB: a cada 15 minutos em dias uteis, 9h as 18h de Brasilia (secao 5.6). */
@Component
public class AtualizacaoCotacoesBcbJob extends AtualizacaoCotacoes {

  public AtualizacaoCotacoesBcbJob(
      BcbPtaxCliente fonte,
      MoedaRepositorio moedas,
      GravacaoDados gravacao,
      CacheConsultas cache,
      Clock relogio,
      PropriedadesWorker props) {
    super(fonte, moedas, gravacao, cache, relogio, props.janelaCotacoesDias());
  }

  @Override
  public String nome() {
    return "AtualizacaoCotacoesBcbJob";
  }
}
