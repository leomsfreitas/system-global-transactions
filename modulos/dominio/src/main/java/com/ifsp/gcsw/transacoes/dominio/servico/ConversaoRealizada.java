package com.ifsp.gcsw.transacoes.dominio.servico;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConversaoRealizada(
    String moedaOrigem,
    String moedaDestino,
    BigDecimal valorOriginal,
    BigDecimal taxaCambio,
    BigDecimal fatorGeopolitico,
    BigDecimal valorConvertido,
    OffsetDateTime dataHora,
    OffsetDateTime dataReferencia,
    String fonte) {}
