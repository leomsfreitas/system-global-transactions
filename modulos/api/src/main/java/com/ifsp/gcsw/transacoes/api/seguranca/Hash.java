package com.ifsp.gcsw.transacoes.api.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class Hash {

  private Hash() {}

  static String sha256(String valor) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
