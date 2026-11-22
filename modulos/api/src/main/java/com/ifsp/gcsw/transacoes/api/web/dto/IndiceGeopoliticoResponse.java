package com.ifsp.gcsw.transacoes.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndiceGeopoliticoResponse(
    String pais, BigDecimal indiceRisco, BigDecimal fatorAjuste, LocalDate dataReferencia) {}
