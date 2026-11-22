package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.conectores.FonteIndicadores;
import com.ifsp.gcsw.transacoes.conectores.FonteRiscoPolitico;
import com.ifsp.gcsw.transacoes.conectores.modelo.IndicadorColetado;
import com.ifsp.gcsw.transacoes.conectores.modelo.PaisConsulta;
import com.ifsp.gcsw.transacoes.conectores.modelo.ResultadoColeta;
import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.repositorio.PaisRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Indicadores (inflacao, juros, PIB) e estabilidade politica de todas as fontes, uma vez por dia.
 * Falha de uma fonte nao impede as outras nem apaga dado existente (secao 6.5).
 */
@Component
public class AtualizacaoIndicadoresJob implements Job {

  private static final Logger LOG = LoggerFactory.getLogger(AtualizacaoIndicadoresJob.class);

  private final List<FonteIndicadores> fontesIndicadores;
  private final List<FonteRiscoPolitico> fontesRisco;
  private final PaisRepositorio paises;
  private final GravacaoDados gravacao;
  private final CacheConsultas cache;

  public AtualizacaoIndicadoresJob(
      List<FonteIndicadores> fontesIndicadores,
      List<FonteRiscoPolitico> fontesRisco,
      PaisRepositorio paises,
      GravacaoDados gravacao,
      CacheConsultas cache) {
    this.fontesIndicadores = fontesIndicadores;
    this.fontesRisco = fontesRisco;
    this.paises = paises;
    this.gravacao = gravacao;
    this.cache = cache;
  }

  @Override
  public String nome() {
    return "AtualizacaoIndicadoresJob";
  }

  @Override
  public ResultadoJob executar() {
    List<PaisConsulta> consulta =
        paises.findAll().stream()
            .map(p -> new PaisConsulta(p.getCodigoIso(), p.getCodigoIso3()))
            .toList();
    List<ColetaNomeada> coletas = new ArrayList<>();
    fontesIndicadores.forEach(f ->
        coletas.add(
            new ColetaNomeada(f.codigo(), f.getClass().getSimpleName(), () -> f.buscar(consulta))));
    fontesRisco.forEach(f ->
        coletas.add(
            new ColetaNomeada(f.codigo(), f.getClass().getSimpleName(), () -> f.buscar(consulta))));

    ResultadoJob total = ResultadoJob.VAZIO;
    for (ColetaNomeada coleta : coletas) {
      total = total.somar(executar(coleta));
    }
    if (total.gravados() > 0) {
      cache.invalidarPorPrefixo(ChavesCache.PREFIXO_INDICADORES);
    }
    return total;
  }

  private ResultadoJob executar(ColetaNomeada coleta) {
    if (!gravacao.fonteAtiva(coleta.fonte())) {
      LOG.info("Fonte {} desativada; {} ignorado", coleta.fonte(), coleta.descricao());
      return ResultadoJob.VAZIO;
    }
    try {
      ResultadoColeta<IndicadorColetado> resultado = coleta.buscar().get();
      Contador contador = new Contador();
      for (IndicadorColetado i : resultado.itens()) {
        contador.registrar(
            gravacao.inserirIndicador(
                i.pais(), TipoIndicador.valueOf(i.tipo()), i.valor(), i.periodo(), i.fonte()));
      }
      return new ResultadoJob(
          resultado.lidos(), contador.gravados, contador.ignorados, resultado.descartados(), 0);
    } catch (RuntimeException e) {
      LOG.error("Falha ao coletar {} ({}): {}", coleta.descricao(), coleta.fonte(), e.getMessage());
      return ResultadoJob.falha();
    }
  }

  private record ColetaNomeada(
      String fonte, String descricao, Supplier<ResultadoColeta<IndicadorColetado>> buscar) {}
}
