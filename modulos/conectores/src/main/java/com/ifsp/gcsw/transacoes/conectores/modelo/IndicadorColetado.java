package com.ifsp.gcsw.transacoes.conectores.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicador coletado. tipo: INFLACAO, JUROS, PIB ou ESTABILIDADE_POLITICA. periodo: primeiro dia do
 * mes ou do ano de referencia.
 */
public record IndicadorColetado(
    String pais, String tipo, BigDecimal valor, LocalDate periodo, String fonte) {}
