package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pais")
public class Pais {

  @Id private Long id;

  @Column(name = "codigo_iso", nullable = false, length = 2)
  private String codigoIso;

  @Column(name = "codigo_iso3", length = 3)
  private String codigoIso3;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private String regiao;

  protected Pais() {}

  public Long getId() {
    return id;
  }

  public String getCodigoIso() {
    return codigoIso;
  }

  public String getCodigoIso3() {
    return codigoIso3;
  }

  public String getNome() {
    return nome;
  }

  public String getRegiao() {
    return regiao;
  }
}
