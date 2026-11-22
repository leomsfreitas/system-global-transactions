package com.ifsp.gcsw.transacoes.api.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record ConversaoRequest(
    @NotNull(message = "e obrigatorio")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de 3 letras")
        String moedaOrigem,
    @NotNull(message = "e obrigatorio")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de 3 letras")
        String moedaDestino,
    @NotNull(message = "e obrigatorio")
        @DecimalMin(value = "0", inclusive = false, message = "deve ser maior que zero")
        @DecimalMax(value = "1000000000000", message = "deve ser no maximo 1000000000000")
        @Digits(integer = 13, fraction = 6, message = "deve ter no maximo 6 casas decimais")
        BigDecimal valor,
    Boolean aplicarAjusteGeopolitico) {

  public boolean ajusteAtivo() {
    return aplicarAjusteGeopolitico == null || aplicarAjusteGeopolitico;
  }
}
