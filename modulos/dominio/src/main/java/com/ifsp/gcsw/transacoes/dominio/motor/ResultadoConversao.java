package com.ifsp.gcsw.transacoes.dominio.motor;

import java.math.BigDecimal;

/** Resultado puro do motor: valores ja arredondados. */
public record ResultadoConversao(
    BigDecimal valorOriginal,
    BigDecimal taxaCambio,
    BigDecimal fatorGeopolitico,
    BigDecimal valorConvertido) {}
