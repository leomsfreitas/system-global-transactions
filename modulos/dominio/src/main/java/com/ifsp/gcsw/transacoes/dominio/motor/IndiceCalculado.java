package com.ifsp.gcsw.transacoes.dominio.motor;

import java.math.BigDecimal;

public record IndiceCalculado(
    BigDecimal riscoPolitico,
    BigDecimal riscoEconomico,
    BigDecimal indiceRisco,
    BigDecimal fatorAjuste) {}
