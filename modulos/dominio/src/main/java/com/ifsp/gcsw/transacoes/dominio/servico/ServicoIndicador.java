package com.ifsp.gcsw.transacoes.dominio.servico;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesCache;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import com.ifsp.gcsw.transacoes.dominio.repositorio.IndicadorEconomicoRepositorio;
import com.ifsp.gcsw.transacoes.dominio.repositorio.PaisRepositorio;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicoIndicador {

  private static final List<String> TIPOS_PUBLICOS =
      Arrays.stream(TipoIndicador.values()).filter(TipoIndicador::publico).map(Enum::name).toList();

  private final IndicadorEconomicoRepositorio repositorio;
  private final PaisRepositorio paises;
  private final CacheConsultas cache;
  private final PropriedadesCache propsCache;

  public ServicoIndicador(
      IndicadorEconomicoRepositorio repositorio,
      PaisRepositorio paises,
      CacheConsultas cache,
      PropriedadesCache propsCache) {
    this.repositorio = repositorio;
    this.paises = paises;
    this.cache = cache;
    this.propsCache = propsCache;
  }

  /** Valor mais recente de cada indicador publico (INFLACAO, JUROS, PIB) do pais (ADR 0015). */
  public List<IndicadorResumo> listar(String pais) {
    return cache.obterLista(
        ChavesCache.indicadores(pais),
        IndicadorResumo.class,
        propsCache.ttlIndicadores(),
        () -> {
          if (paises.findByCodigoIso(pais).isEmpty()) {
            throw new RecursoNaoEncontradoException("Pais nao encontrado: " + pais);
          }
          List<IndicadorResumo> indicadores =
              repositorio.maisRecentesPorTipo(pais, TIPOS_PUBLICOS).stream()
                  .map(i -> new IndicadorResumo(i.getTipo(), i.getValor(), i.getPeriodo()))
                  .toList();
          if (indicadores.isEmpty()) {
            throw new RecursoNaoEncontradoException(
                "Nenhum indicador economico para o pais " + pais);
          }
          return indicadores;
        });
  }
}
