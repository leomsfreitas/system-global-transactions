package com.ifsp.gcsw.transacoes.dominio.servico;

import com.ifsp.gcsw.transacoes.dominio.estrategia.EstrategiaAjusteGeopolitico;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoIndisponivelException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.modelo.Conversao;
import com.ifsp.gcsw.transacoes.dominio.motor.MotorConversao;
import com.ifsp.gcsw.transacoes.dominio.motor.ResultadoConversao;
import com.ifsp.gcsw.transacoes.dominio.repositorio.ConversaoRepositorio;
import com.ifsp.gcsw.transacoes.dominio.taxa.TaxaResolvida;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

/**
 * Orquestra a conversao (secao 6.3): valida moedas, obtem a taxa, obtem o fator pela estrategia,
 * chama o motor, grava a auditoria e devolve o resultado.
 */
@Service
public class ServicoConversao {

  private final ServicoMoeda servicoMoeda;
  private final ServicoCotacao servicoCotacao;
  private final ServicoIndiceGeopolitico servicoIndice;
  private final EstrategiaAjusteGeopolitico estrategia;
  private final MotorConversao motor;
  private final ConversaoRepositorio auditoria;
  private final Clock relogio;

  public ServicoConversao(
      ServicoMoeda servicoMoeda,
      ServicoCotacao servicoCotacao,
      ServicoIndiceGeopolitico servicoIndice,
      EstrategiaAjusteGeopolitico estrategia,
      MotorConversao motor,
      ConversaoRepositorio auditoria,
      Clock relogio) {
    this.servicoMoeda = servicoMoeda;
    this.servicoCotacao = servicoCotacao;
    this.servicoIndice = servicoIndice;
    this.estrategia = estrategia;
    this.motor = motor;
    this.auditoria = auditoria;
    this.relogio = relogio;
  }

  public ConversaoRealizada converter(
      String origem,
      String destino,
      BigDecimal valor,
      boolean aplicarAjuste,
      String apiKeyHashPrefixo) {
    MoedaCadastrada moedaOrigem = servicoMoeda.buscar(origem);
    MoedaCadastrada moedaDestino = servicoMoeda.buscar(destino);
    TaxaResolvida taxa = servicoCotacao.atual(origem, destino);
    BigDecimal fator =
        aplicarAjuste
            ? estrategia.fator(moedaOrigem.pais(), moedaDestino.pais(), this::fatorDoPais)
            : MotorConversao.SEM_AJUSTE;

    ResultadoConversao resultado =
        motor.converter(valor, taxa.valor(), fator, moedaDestino.casasDecimais());
    OffsetDateTime agora = OffsetDateTime.now(relogio);

    auditoria.save(
        new Conversao(
            moedaOrigem.id(),
            moedaDestino.id(),
            resultado.valorOriginal(),
            resultado.taxaCambio(),
            resultado.fatorGeopolitico(),
            resultado.valorConvertido(),
            agora,
            taxa.fonteId(),
            apiKeyHashPrefixo));

    return new ConversaoRealizada(
        origem,
        destino,
        resultado.valorOriginal(),
        resultado.taxaCambio(),
        resultado.fatorGeopolitico(),
        resultado.valorConvertido(),
        agora,
        taxa.dataReferencia(),
        taxa.fonte());
  }

  private BigDecimal fatorDoPais(String pais) {
    try {
      return servicoIndice.atual(pais).fatorAjuste();
    } catch (RecursoNaoEncontradoException e) {
      throw new DadoIndisponivelException("Indice geopolitico indisponivel para o pais " + pais);
    }
  }
}
