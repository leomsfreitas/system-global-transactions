package com.ifsp.gcsw.transacoes.conectores.fmi;

import java.time.LocalDate;

/** Acesso de teste ao metodo package-private de interpretacao de periodo. */
public final class FmiClienteTestAcesso {
  private FmiClienteTestAcesso() {}

  public static LocalDate periodo(String texto) {
    return FmiCliente.periodo(texto);
  }
}
