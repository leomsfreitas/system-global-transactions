package com.ifsp.gcsw.transacoes.api.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ConversaoResponse(
    String moedaOrigem,
    String moedaDestino,
    BigDecimal valorOriginal,
    BigDecimal taxaCambio,
    BigDecimal fatorGeopolitico,
    BigDecimal valorConvertido,
    OffsetDateTime dataHora) {}
