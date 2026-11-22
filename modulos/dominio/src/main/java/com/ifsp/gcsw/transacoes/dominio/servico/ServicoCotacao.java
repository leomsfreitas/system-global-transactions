package com.ifsp.gcsw.transacoes.dominio.servico;

import com.ifsp.gcsw.transacoes.dominio.cache.CacheConsultas;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesCache;
import com.ifsp.gcsw.transacoes.dominio.config.PropriedadesCotacao;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoDesatualizadoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RequisicaoInvalidaException;
import com.ifsp.gcsw.transacoes.dominio.taxa.ResolvedorTaxaCambio;
import com.ifsp.gcsw.transacoes.dominio.taxa.TaxaResolvida;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServicoCotacao {

  private final ResolvedorTaxaCambio resolvedor;
  private final ServicoMoeda servicoMoeda;
  private final CacheConsultas cache;
  private final PropriedadesCache propsCache;
  private final PropriedadesCotacao propsCotacao;

  public ServicoCotacao(
      ResolvedorTaxaCambio resolvedor,
      ServicoMoeda servicoMoeda,
      CacheConsultas cache,
      PropriedadesCache propsCache,
      PropriedadesCotacao propsCotacao) {
    this.resolvedor = resolvedor;
    this.servicoMoeda = servicoMoeda;
    this.cache = cache;
    this.propsCache = propsCache;
    this.propsCotacao = propsCotacao;
  }

  /** Cotacao vigente do par, com cache e regra de frescor (secoes 5.4 e 5.6). */
  public TaxaResolvida atual(String base, String destino) {
    servicoMoeda.buscar(base);
    servicoMoeda.buscar(destino);
    String chave = ChavesCache.cotacaoAtual(base, destino);
    TaxaResolvida taxa =
        cache.obter(
            chave,
            TaxaResolvida.class,
            propsCache.ttlCotacao(),
            () -> resolvedor.resolver(base, destino));
    try {
      resolvedor.exigirFrescor(taxa);
      return taxa;
    } catch (DadoDesatualizadoException e) {
      // O valor do cache envelheceu: descarta e tenta de novo pela origem.
      cache.invalidar(chave);
      return resolvedor.resolver(base, destino);
    }
  }

  public List<TaxaResolvida> historico(
      String base, String destino, LocalDate inicio, LocalDate fim) {
    if (inicio == null || fim == null) {
      throw new RequisicaoInvalidaException("dataInicio e dataFim sao obrigatorios");
    }
    if (fim.isBefore(inicio)) {
      throw new RequisicaoInvalidaException("dataFim deve ser maior ou igual a dataInicio");
    }
    long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
    if (dias > propsCotacao.historicoIntervaloMaximoDias()) {
      throw new RequisicaoInvalidaException(
          "O intervalo maximo do historico e de %d dias"
              .formatted(propsCotacao.historicoIntervaloMaximoDias()));
    }
    servicoMoeda.buscar(base);
    servicoMoeda.buscar(destino);
    List<TaxaResolvida> historico = resolvedor.historico(base, destino, inicio, fim);
    if (historico.isEmpty()) {
      throw new RecursoNaoEncontradoException(
          "Nenhuma cotacao de %s/%s entre %s e %s".formatted(base, destino, inicio, fim));
    }
    return historico;
  }
}
