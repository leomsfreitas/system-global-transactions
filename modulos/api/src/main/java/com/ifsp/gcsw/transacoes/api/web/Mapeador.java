package com.ifsp.gcsw.transacoes.api.web;

import com.ifsp.gcsw.transacoes.api.web.dto.ConversaoResponse;
import com.ifsp.gcsw.transacoes.api.web.dto.CotacaoResponse;
import com.ifsp.gcsw.transacoes.api.web.dto.IndicadorEconomicoResponse;
import com.ifsp.gcsw.transacoes.api.web.dto.IndiceGeopoliticoResponse;
import com.ifsp.gcsw.transacoes.api.web.dto.MoedaResponse;
import com.ifsp.gcsw.transacoes.dominio.servico.ConversaoRealizada;
import com.ifsp.gcsw.transacoes.dominio.servico.IndicadorResumo;
import com.ifsp.gcsw.transacoes.dominio.servico.IndiceAtual;
import com.ifsp.gcsw.transacoes.dominio.servico.MoedaCadastrada;
import com.ifsp.gcsw.transacoes.dominio.taxa.TaxaResolvida;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Mapeamento manual dominio -> DTO (ADR 0003: sem MapStruct). */
final class Mapeador {

  private Mapeador() {}

  static ConversaoResponse conversao(ConversaoRealizada c) {
    return new ConversaoResponse(
        c.moedaOrigem(),
        c.moedaDestino(),
        c.valorOriginal(),
        c.taxaCambio(),
        c.fatorGeopolitico(),
        c.valorConvertido(),
        utc(c.dataHora()));
  }

  static CotacaoResponse cotacao(TaxaResolvida t) {
    return new CotacaoResponse(
        t.base(), t.destino(), t.valor(), utc(t.dataReferencia()), t.fonte());
  }

  static MoedaResponse moeda(MoedaCadastrada m) {
    return new MoedaResponse(m.codigo(), m.nome(), m.pais());
  }

  static IndicadorEconomicoResponse indicador(IndicadorResumo i) {
    // NUMERIC(24,6) traz zeros a direita (4.630000); o indicador e devolvido com a escala natural.
    return new IndicadorEconomicoResponse(i.tipo(), i.valor().stripTrailingZeros(), i.periodo());
  }

  static IndiceGeopoliticoResponse indice(IndiceAtual i) {
    return new IndiceGeopoliticoResponse(
        i.pais(), i.indiceRisco(), i.fatorAjuste(), i.dataReferencia());
  }

  static String dataReferencia(OffsetDateTime dataHora) {
    // ISO 8601 com segundos sempre presentes (RFC 3339); toString() omite ':00'.
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(utc(dataHora));
  }

  static OffsetDateTime utc(OffsetDateTime dataHora) {
    return dataHora == null ? null : dataHora.withOffsetSameInstant(ZoneOffset.UTC);
  }
}
