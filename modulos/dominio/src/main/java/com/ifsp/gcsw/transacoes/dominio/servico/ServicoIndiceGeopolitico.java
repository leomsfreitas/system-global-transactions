package com.ifsp.gcsw.transacoes.dominio.servico;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesCache;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.repositorio.IndiceGeopoliticoRepositorio;
import com.ifsp.gcsw.transacoes.dominio.repositorio.PaisRepositorio;
import org.springframework.stereotype.Service;

@Service
public class ServicoIndiceGeopolitico {

  private final IndiceGeopoliticoRepositorio repositorio;
  private final PaisRepositorio paises;
  private final CacheConsultas cache;
  private final PropriedadesCache propsCache;

  public ServicoIndiceGeopolitico(
      IndiceGeopoliticoRepositorio repositorio,
      PaisRepositorio paises,
      CacheConsultas cache,
      PropriedadesCache propsCache) {
    this.repositorio = repositorio;
    this.paises = paises;
    this.cache = cache;
    this.propsCache = propsCache;
  }

  /** Indice mais recente do pais. A API so le; o calculo e do worker (secao 5.1). */
  public IndiceAtual atual(String pais) {
    return cache.obter(
        ChavesCache.indiceAtual(pais),
        IndiceAtual.class,
        propsCache.ttlIndice(),
        () ->
            repositorio
                .maisRecente(pais)
                .map(i ->
                    new IndiceAtual(
                        pais, i.getIndiceRisco(), i.getFatorAjuste(), i.getDataReferencia()))
                .orElseThrow(() -> naoEncontrado(pais)));
  }

  private RecursoNaoEncontradoException naoEncontrado(String pais) {
    if (paises.findByCodigoIso(pais).isEmpty()) {
      return new RecursoNaoEncontradoException("Pais nao encontrado: " + pais);
    }
    return new RecursoNaoEncontradoException("Nenhum indice geopolitico para o pais " + pais);
  }
}
