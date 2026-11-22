package com.ifsp.gcsw.transacoes.worker.job;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesAjusteGeopolitico;
import com.ifsp.gcsw.transacoes.dominio.modelo.Pais;
import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import com.ifsp.gcsw.transacoes.dominio.motor.CalculadoraIndiceGeopolitico;
import com.ifsp.gcsw.transacoes.dominio.motor.CalculadoraIndiceGeopolitico.ComponenteAgregado;
import com.ifsp.gcsw.transacoes.dominio.motor.IndiceCalculado;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.repositorio.IndicadorEconomicoRepositorio;
import com.ifsp.gcsw.transacoes.dominio.repositorio.IndicadorProjecao;
import com.ifsp.gcsw.transacoes.dominio.repositorio.PaisRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Calcula o indice de risco e o fator de ajuste de cada pais (secao 5.1) e dos agregados como EU
 * (secao 5.3). Grava uma nova linha so quando ha periodo novo: data_referencia e o periodo mais
 * recente entre os insumos, e a unicidade (pais, data_referencia, fonte) torna o job idempotente.
 */
@Component
public class CalculoIndiceGeopoliticoJob implements Job {

  private static final Logger LOG = LoggerFactory.getLogger(CalculoIndiceGeopoliticoJob.class);
  private static final String FONTE = "BANCO_MUNDIAL";
  private static final List<String> TIPOS =
      List.of(
          TipoIndicador.ESTABILIDADE_POLITICA.name(),
          TipoIndicador.INFLACAO.name(),
          TipoIndicador.PIB.name());

  private final PaisRepositorio paises;
  private final IndicadorEconomicoRepositorio indicadores;
  private final CalculadoraIndiceGeopolitico calculadora;
  private final PropriedadesAjusteGeopolitico props;
  private final GravacaoDados gravacao;
  private final CacheConsultas cache;

  public CalculoIndiceGeopoliticoJob(
      PaisRepositorio paises,
      IndicadorEconomicoRepositorio indicadores,
      CalculadoraIndiceGeopolitico calculadora,
      PropriedadesAjusteGeopolitico props,
      GravacaoDados gravacao,
      CacheConsultas cache) {
    this.paises = paises;
    this.indicadores = indicadores;
    this.calculadora = calculadora;
    this.props = props;
    this.gravacao = gravacao;
    this.cache = cache;
  }

  @Override
  public String nome() {
    return "CalculoIndiceGeopoliticoJob";
  }

  @Override
  public ResultadoJob executar() {
    Map<String, Calculo> calculados = new HashMap<>();
    Contador contador = new Contador();
    int semInsumos = 0;
    for (Pais pais : paises.findAll()) {
      if (props.agregados().containsKey(pais.getCodigoIso())) {
        continue;
      }
      Map<String, IndicadorProjecao> ultimos =
          indicadores.maisRecentesPorTipo(pais.getCodigoIso(), TIPOS).stream()
              .collect(Collectors.toMap(IndicadorProjecao::getTipo, Function.identity()));
      IndicadorProjecao estabilidade = ultimos.get(TipoIndicador.ESTABILIDADE_POLITICA.name());
      IndicadorProjecao inflacao = ultimos.get(TipoIndicador.INFLACAO.name());
      if (estabilidade == null || inflacao == null) {
        semInsumos++;
        continue;
      }
      IndiceCalculado indice = calculadora.calcular(estabilidade.getValor(), inflacao.getValor());
      LocalDate referencia = maisRecente(estabilidade.getPeriodo(), inflacao.getPeriodo());
      IndicadorProjecao pib = ultimos.get(TipoIndicador.PIB.name());
      calculados.put(
          pais.getCodigoIso(),
          new Calculo(indice, referencia, pib == null ? null : pib.getValor()));
      contador.registrar(
          gravacao.inserirIndice(
              pais.getCodigoIso(), indice.indiceRisco(), indice.fatorAjuste(), referencia, FONTE));
    }

    props
        .agregados()
        .forEach((agregado, membros) -> {
          List<ComponenteAgregado> componentes = new ArrayList<>();
          LocalDate referencia = null;
          for (String membro : membros) {
            Calculo c = calculados.get(membro);
            if (c != null) {
              componentes.add(new ComponenteAgregado(membro, c.indice().indiceRisco(), c.pib()));
              referencia = maisRecente(referencia, c.referencia());
            }
          }
          if (componentes.isEmpty()) {
            LOG.warn("Agregado {} sem membros com indice calculado", agregado);
            return;
          }
          BigDecimal indice = calculadora.mediaAgregada(componentes);
          contador.registrar(
              gravacao.inserirIndice(
                  agregado, indice, calculadora.fator(indice), referencia, FONTE));
        });

    if (contador.gravados > 0) {
      cache.invalidarPorPrefixo(ChavesCache.PREFIXO_INDICE);
    }
    int lidos = contador.gravados + contador.ignorados + semInsumos;
    return new ResultadoJob(lidos, contador.gravados, contador.ignorados, semInsumos, 0);
  }

  private static LocalDate maisRecente(LocalDate a, LocalDate b) {
    if (a == null) {
      return b;
    }
    return b == null || a.isAfter(b) ? a : b;
  }

  private record Calculo(IndiceCalculado indice, LocalDate referencia, BigDecimal pib) {}
}
