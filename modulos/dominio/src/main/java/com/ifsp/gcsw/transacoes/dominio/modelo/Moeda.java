package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "moeda")
public class Moeda {

  @Id private Long id;

  @Column(name = "codigo_iso", nullable = false, length = 3)
  private String codigoIso;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private String simbolo;

  @Column(name = "casas_decimais", nullable = false)
  private short casasDecimais;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "pais_id")
  private Pais pais;

  protected Moeda() {}

  public Long getId() {
    return id;
  }

  public String getCodigoIso() {
    return codigoIso;
  }

  public String getNome() {
    return nome;
  }

  public String getSimbolo() {
    return simbolo;
  }

  public int getCasasDecimais() {
    return casasDecimais;
  }

  public Pais getPais() {
    return pais;
  }
}
