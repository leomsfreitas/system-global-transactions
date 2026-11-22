package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.conectores.FonteCotacoes;
import com.ifsp.gcsw.transacoes.conectores.modelo.CotacaoColetada;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.modelo.Moeda;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.repositorio.MoedaRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base dos jobs de cotacao: coleta a janela recente, grava sem sobrescrever e invalida o cache. */
abstract class AtualizacaoCotacoes implements Job {

  private static final Logger LOG = LoggerFactory.getLogger(AtualizacaoCotacoes.class);

  private final FonteCotacoes fonte;
  private final MoedaRepositorio moedas;
  private final GravacaoDados gravacao;
  private final CacheConsultas cache;
  private final Clock relogio;
  private final int janelaDias;

  AtualizacaoCotacoes(
      FonteCotacoes fonte,
      MoedaRepositorio moedas,
      GravacaoDados gravacao,
      CacheConsultas cache,
      Clock relogio,
      int janelaDias) {
    this.fonte = fonte;
    this.moedas = moedas;
    this.gravacao = gravacao;
    this.cache = cache;
    this.relogio = relogio;
    this.janelaDias = janelaDias;
  }

  @Override
  public ResultadoJob executar() {
    if (!gravacao.fonteAtiva(fonte.codigo())) {
      LOG.info(
          "Fonte {} desativada em fonte_dados.ativa; job {} nao executado", fonte.codigo(), nome());
      return ResultadoJob.VAZIO;
    }
    Set<String> codigos =
        moedas.findAll().stream().map(Moeda::getCodigoIso).collect(Collectors.toSet());
    LocalDate hoje = LocalDate.now(relogio);
    ResultadoColeta<CotacaoColetada> coleta =
        fonte.buscar(hoje.minusDays(janelaDias), hoje, codigos);
    Contador contador = new Contador();
    for (CotacaoColetada c : coleta.itens()) {
      contador.registrar(
          gravacao.inserirCotacao(c.base(), c.destino(), c.valor(), c.dataHora(), c.fonte()));
    }
    if (contador.gravados > 0) {
      // Qualquer par pode depender destas cotacoes (inverso e taxa cruzada).
      cache.invalidarPorPrefixo(ChavesCache.PREFIXO_COTACAO);
    }
    return new ResultadoJob(
        coleta.lidos(), contador.gravados, contador.ignorados, coleta.descartados(), 0);
  }
}
