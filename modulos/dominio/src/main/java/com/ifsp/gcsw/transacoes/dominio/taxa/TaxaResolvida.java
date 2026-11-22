package com.ifsp.gcsw.transacoes.dominio.taxa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Taxa base -> destino com 6 casas. dataReferencia e a data da cotacao usada (na cruzada, a mais
 * antiga das duas pernas). fonte segue a secao 5.4: "BCB", "BCE" ou "Cruzada: BCB".
 */
public record TaxaResolvida(
    String base,
    String destino,
    BigDecimal valor,
    OffsetDateTime dataReferencia,
    String fonte,
    Long fonteId,
    MetodoTaxa metodo) {}
