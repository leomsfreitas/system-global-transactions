package com.ifsp.gcsw.transacoes.dominio.taxa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Uma cotacao gravada: base -> destino = valor, publicada por fonte em dataHora. */
public record CotacaoVigente(
    String base,
    String destino,
    BigDecimal valor,
    OffsetDateTime dataHora,
    String fonte,
    Long fonteId) {}
