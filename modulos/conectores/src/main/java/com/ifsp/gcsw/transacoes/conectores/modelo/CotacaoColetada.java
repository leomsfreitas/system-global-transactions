package com.ifsp.gcsw.transacoes.conectores.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Cotacao exatamente como a fonte publica: 1 base = valor destino. */
public record CotacaoColetada(
    String base, String destino, BigDecimal valor, OffsetDateTime dataHora, String fonte) {}
