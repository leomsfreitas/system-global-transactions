package com.ifsp.gcsw.transacoes.api.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CotacaoResponse(
    String base, String destino, BigDecimal valor, OffsetDateTime dataHora, String fonte) {}
