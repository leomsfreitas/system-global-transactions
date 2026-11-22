package com.ifsp.gcsw.transacoes.dominio.servico;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorResumo(String tipo, BigDecimal valor, LocalDate periodo) {}
