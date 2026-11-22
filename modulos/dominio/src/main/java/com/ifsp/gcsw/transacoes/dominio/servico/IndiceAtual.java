package com.ifsp.gcsw.transacoes.dominio.servico;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndiceAtual(
    String pais, BigDecimal indiceRisco, BigDecimal fatorAjuste, LocalDate dataReferencia) {}
