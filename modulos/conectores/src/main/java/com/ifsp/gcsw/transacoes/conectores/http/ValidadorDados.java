package com.ifsp.gcsw.transacoes.conectores.http;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Validacao do que chega das fontes (secao 6.5): valor positivo, data plausivel, moeda conhecida.
 * Dado invalido e descartado e contado pelo conector.
 */
public class ValidadorDados {

  private static final LocalDate DATA_MINIMA = LocalDate.of(1950, 1, 1);

  private final Clock relogio;

  public ValidadorDados(Clock relogio) {
    this.relogio = relogio;
  }

  public boolean cotacaoValida(BigDecimal valor, OffsetDateTime dataHora) {
    return valor != null
        && valor.signum() > 0
        && dataHora != null
        && dataPlausivel(dataHora.toLocalDate());
  }

  /** Datas no futuro (alem de amanha, por causa de fuso) ou anteriores a 1950 sao implausiveis. */
  public boolean dataPlausivel(LocalDate data) {
    return data != null
        && !data.isBefore(DATA_MINIMA)
        && !data.isAfter(LocalDate.now(relogio).plusDays(1));
  }

  public static boolean codigoMoedaValido(String codigo) {
    return codigo != null && codigo.matches("^[A-Z]{3}$");
  }
}
