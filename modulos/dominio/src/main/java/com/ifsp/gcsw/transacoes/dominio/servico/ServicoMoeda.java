package com.ifsp.gcsw.transacoes.dominio.servico;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesCache;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.repositorio.MoedaRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicoMoeda {

  private final MoedaRepositorio repositorio;
  private final CacheConsultas cache;
  private final PropriedadesCache propsCache;

  public ServicoMoeda(
      MoedaRepositorio repositorio, CacheConsultas cache, PropriedadesCache propsCache) {
    this.repositorio = repositorio;
    this.cache = cache;
    this.propsCache = propsCache;
  }

  public List<MoedaCadastrada> listar() {
    return cache.obterLista(
        ChavesCache.MOEDAS,
        MoedaCadastrada.class,
        propsCache.ttlMoedas(),
        () ->
            repositorio.listarComPais().stream()
                .map(m ->
                    new MoedaCadastrada(
                        m.getId(),
                        m.getCodigoIso(),
                        m.getNome(),
                        m.getCasasDecimais(),
                        m.getPais().getCodigoIso()))
                .toList());
  }

  public MoedaCadastrada buscar(String codigo) {
    return listar().stream()
        .filter(m -> m.codigo().equals(codigo))
        .findFirst()
        .orElseThrow(() -> new RecursoNaoEncontradoException("Moeda nao suportada: " + codigo));
  }
}
