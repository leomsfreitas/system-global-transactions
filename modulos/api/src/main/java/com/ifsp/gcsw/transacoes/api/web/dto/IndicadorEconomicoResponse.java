package com.ifsp.gcsw.transacoes.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorEconomicoResponse(String tipo, BigDecimal valor, LocalDate periodo) {}
